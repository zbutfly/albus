package net.butfly.bus.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.GenericUtils;
import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.ReflectionUtils.MethodInfo;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.albacore.utils.async.Task.Callback;
import net.butfly.albacore.utils.async.Task.ExceptionHandler;
import net.butfly.bus.Bus;
import net.butfly.bus.Error;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.config.Config;
import net.butfly.bus.config.bean.invoker.InvokerBean;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;
import net.butfly.bus.filter.Filter;
import net.butfly.bus.filter.FilterBase;
import net.butfly.bus.filter.FilterChain;
import net.butfly.bus.filter.FilterContext;
import net.butfly.bus.impl.BusFactory.Mode;
import net.butfly.bus.invoker.AbstractLocalInvoker;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.policy.Router;
import net.butfly.bus.service.InternalFacade;
import net.butfly.bus.utils.BusTask;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.TXUtils;

abstract class BasicBusImpl implements Bus, InternalFacade {
	private static final long serialVersionUID = 3149085159221930900L;
	private final String id;
	private Config config;
	private Router router;
	protected FilterChain chain;

	private String[] supportedTXs;
	private Mode mode;
	private Filter firstFilter, lastFilter;

	public BasicBusImpl(Mode mode) {
		this(null, mode);
	}

	public BasicBusImpl(String configLocation, Mode mode) {
		this.mode = mode;
		this.config = BusFactoryImpl.createConfiguration(configLocation, mode);
		this.router = BusFactoryImpl.createRouter(this.config);
		this.firstFilter = new FirstFilter();
		this.lastFilter = new LastFilter();
		this.chain = new FilterChain(this.firstFilter, config.getFilterList(), this.lastFilter, this);
		this.id = KeyUtils.objectId();

		// initialize tx supporting status
		// TODO: reg in database
		Set<String> txs = new HashSet<String>();
		for (String id : config.getAllNodeIDs())
			txs.addAll(Arrays.asList(config.getInvoker(id).supportedTXs()));
		this.supportedTXs = txs.toArray(new String[txs.size()]);
	}

	public String id() {
		return this.id;
	}

	public boolean isSupported(String requestTX) {
		return TXUtils.matching(this.supportedTXs(), requestTX) >= 0;

	}

	@Override
	public String[] supportedTXs() {
		return this.supportedTXs;
	}

	@SuppressWarnings("rawtypes")
	MethodInfo invokeInfo(String code, String version) {
		InvokerBean ivkb = this.router.route(code, this.config.getInvokers());
		if (null == ivkb) return null;
		Invoker<?> ivk = BusFactoryImpl.getInvoker(ivkb, mode);
		if (null == ivk) return null;
		if (!(ivk instanceof AbstractLocalInvoker))
			throw new UnsupportedOperationException("Only local invokers support real method fetching by options.");
		Method m = ((AbstractLocalInvoker) ivk).getMethod(code, version);
		if (null == m) return null;
		Class<?> r = m.getReturnType();
		if (r != null) {
			if (r.isArray()) r = r.getComponentType();
			else if (Map.class.isAssignableFrom(r)) r = null;
			else if (Collection.class.isAssignableFrom(r)) r = GenericUtils.getGenericParamClass(r, Collection.class, "E");
			else if (Enumeration.class.isAssignableFrom(r)) r = GenericUtils.getGenericParamClass(r, Enumeration.class, "E");
			else if (Iterable.class.isAssignableFrom(r)) r = GenericUtils.getGenericParamClass(r, Iterable.class, "T");
		}
		return new MethodInfo(m.getParameterTypes(), r);
	}

	protected class FirstFilter extends FilterBase implements Filter {
		public void before(FilterContext context) {
			switch (mode) {
			case CLIENT:
				context.request().context(Context.serialize(Context.toMap()));
				break;
			case SERVER:
				Context.merge(Context.deserialize(context.request().context()));
				break;
			}
			Invoker<?> invoker = BusFactoryImpl.getInvoker(router.route(context.request().code(), config.getInvokers()), mode);
			context.request().token(invoker.token());
			context.invoker(invoker);
		}

		@Override
		public void after(FilterContext context) {
			switch (mode) {
			case CLIENT:
				Context.merge(Context.deserialize(context.response().context()));
				break;
			case SERVER:
				context.response().context(Context.serialize(Context.toMap()));
				break;
			}
		}

		@Override
		public void execute(final FilterContext context) throws Exception {
			if (null == context.callback()) {
				try {
					before(context);
					super.execute(context);
					after(context);
					if (mode == Mode.CLIENT && null != context.response().error())
						throw context.response().error().toException();
				} catch (Exception ex) {
					exception(context, ex);
				}
			} else {
				new Task<Response>(new Task.Callable<Response>() {
					@Override
					public Response call() throws Exception {
						before(context);
						FirstFilter.super.execute(context);
						return context.response();
					}
				}, new Task.Callback<Response>() {
					@Override
					public void callback(Response response) {
						if (mode == Mode.CLIENT && null != response.error()) throw new RuntimeException(response.error()
								.toException());
						else {
							after(context);
							context.callback().callback(context.response());
						}
					}
				}).handler(new Task.ExceptionHandler<Response>() {
					@Override
					public Response handle(Exception ex) throws Exception {
						return exception(context, ex);
					}
				}).execute();
			}
		}
	}

	protected class LastFilter extends FilterBase implements Filter {
		private Request request;
		private Options lo;
		private Options[] ro;
		private Task.Callable<Response> invokeTask;

		public void before(FilterContext context) {
			this.request = context.request();
			this.lo = context.invoker().localOptions(context.options());
			this.ro = context.invoker().remoteOptions(context.options());
			this.invokeTask = context.invoker().task(this.request, ro);
		}

		/**
		 * Kernal invoking of this bus.
		 * 
		 * @param options
		 * @throws Exception
		 */
		@Override
		public void execute(final FilterContext context) throws Exception {
			if (null == context.callback()) {
				before(context);
				context.response(new BusTask<Response>(new Task<Response>(invokeTask, null, lo)).handler(
						new ExceptionHandler<Response>() {
							@Override
							public Response handle(Exception exception) throws Exception {
								return exception(context, exception);
							}
						}).execute());
				after(context);
			} else {
				new BusTask<Response>(new Task<Response>(new Task.Callable<Response>() {
					@Override
					public Response call() throws Exception {
						before(context);
						return invokeTask.call();
					}
				}, new Task.Callback<Response>() {
					@Override
					public void callback(Response response) {
						context.response(response);
						after(context);
					}
				}, lo)).handler(new Task.ExceptionHandler<Response>() {
					@Override
					public Response handle(Exception ex) throws Exception {
						Response response = exception(context, ex);
						context.response(response);
						return response;
					}
				}).execute();
			}
		}

		@Override
		public Response exception(FilterContext context, Exception exception) throws Exception {
			if (mode == Mode.SERVER) return new Response(context.request()).error(new Error(Exceptions.unwrap(exception),
					Context.debug()));
			else return super.exception(context, exception);
		}
	}

	protected abstract class ServiceProxy<T> implements InvocationHandler {
		protected Options[] options;

		public ServiceProxy(Options... options) {
			this.options = options;
		}

		public Object invoke(Object obj, Method method, Object[] args) throws Exception {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				return this.invoke(request);
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method ["
					+ method.toString() + "].");
		}

		protected abstract T invoke(Request request) throws Exception;
	}

	private InternalFacade internal;

	@Override
	public long ping() {
		return internal.ping();
	}

	@Override
	public String echo(String echo) {
		return internal.echo(echo);
	}

	@Override
	public void sleep(long ms) {
		internal.sleep(ms);
	}

	protected final void check(final Request request) {
		if (request == null) throw new SystemException(Constants.UserError.BAD_REQUEST, "Request null invalid.");
		if (request.code() == null || "".equals(request.code().trim()))
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx code invalid.");
		if (request.version() == null)
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx version invalid.");
		new FlowNo(request);
		Context.txInfo(TXUtils.TXImpl(request.code(), request.version()));
	}

	abstract <T> Response invoke(Request request, Options... options) throws Exception;

	abstract <T> void invoke(Request request, Callback<T> callback, Options... options) throws Exception;
}
