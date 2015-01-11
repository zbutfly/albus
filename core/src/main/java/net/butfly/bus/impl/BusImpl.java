package net.butfly.bus.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.GenericUtils;
import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus;
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
import net.butfly.bus.invoker.AbstractLocalInvoker;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.invoker.InvokerFactory;
import net.butfly.bus.invoker.ParameterInfo;
import net.butfly.bus.policy.Routeable;
import net.butfly.bus.policy.Router;
import net.butfly.bus.service.InternalFacade;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.RequestWrapper;
import net.butfly.bus.utils.TXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Signal;

class BusImpl implements InternalFacade, Routeable, Bus {
	private static final long serialVersionUID = -4835302344711170159L;
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final String id;
	protected Config config;
	protected Router router;
	protected FilterChain chain;

	private String[] supportedTXs;
	private BusMode mode;

	/* Routine for both client and server */

	public BusImpl(BusMode mode) {
		this(null, mode);
	}

	public BusImpl(String configLocation, BusMode mode) {
		this.mode = mode;
		this.config = BusUtils.createConfiguration(configLocation, mode);
		this.router = BusUtils.createRouter(this.config);
		this.chain = new FilterChain(config.getFilterList(), new InvokerFilter());
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
		return TXUtils.isMatching(this.supportedTXs(), requestTX);

	}

	@Override
	public String[] supportedTXs() {
		return this.supportedTXs;
	}

	@SuppressWarnings("rawtypes")
	public ParameterInfo getParameterInfo(String code, String version) {
		InvokerBean ivkb = BusImpl.this.router.route(code, BusImpl.this.config.getInvokers());
		if (null == ivkb) return null;
		Invoker<?> ivk = InvokerFactory.getInvoker(ivkb);
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
		return new ParameterInfo(m.getParameterTypes(), r);
	}

	// Routines for client
	/* ******************************************************** */

	@Override
	@SuppressWarnings("unchecked")
	public <T, F extends Facade> F getService(Class<F> facadeClass, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, new ServiceProxy<T>(
				options));
	}

	/**
	 * Kernal invoking for this bus.
	 * 
	 * @param options
	 * @return
	 * @throws Exception
	 * @throws Signal
	 */
	public <T> Response invoke(Request request, Options... options) throws Exception {
		if (request == null) throw new SystemException(Constants.UserError.BAD_REQUEST, "Request null invalid.");
		if (request.code() == null || "".equals(request.code().trim()))
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx code invalid.");
		if (request.version() == null)
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx version invalid.");
		new FlowNo(request);
		Context.txInfo(TXUtils.TXImpl(request.code(), request.version()));
		return chain.execute(new RequestWrapper<T>(request, options));
	}

	@Override
	public <T> T invoke(String code, Object[] args, Options... options) throws Exception {
		return this.invoke(TXUtils.TXImpl(code), args, options);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(TX tx, Object[] args, Options... options) throws Exception {
		Response resp = this.invoke(new Request(tx, args), options);
		return (T) resp.result();
	}

	private class ServiceProxy<T> implements InvocationHandler {
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

		@SuppressWarnings("unchecked")
		protected T invoke(Request request) throws Exception {
			return (T) BusImpl.this.invoke(request, options).result();
		}
	}

	/* ******************************************************** */
	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, Task.Callback<T> callback, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass },
				new AsyncServiceProxy<T>(callback, options));
	}

	@Override
	public <T> void invoke(String code, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception {
		this.invoke(TXUtils.TXImpl(code), arguments, callback, options);
	};

	@Override
	public <T> void invoke(TX tx, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception {
		this.invoke(new Request(tx, arguments), callback, options);
	}

	/**
	 * Kernal invoking of callback bus, overlay async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into BusImpl.InvokerFilter for handling.
	 */
	public <T> void invoke(final Request request, Task.Callback<T> callback, final Options... options) throws Exception {
		if (request == null) throw new SystemException(Constants.UserError.BAD_REQUEST, "Request null invalid.");
		if (request.code() == null || "".equals(request.code().trim()))
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx code invalid.");
		if (request.version() == null)
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx version invalid.");
		new FlowNo(request);
		Context.txInfo(TXUtils.TXImpl(request.code(), request.version()));
		chain.execute(new RequestWrapper<T>(request, callback, options));
	}

	private class AsyncServiceProxy<T> extends ServiceProxy<T> implements InvocationHandler {
		protected Task.Callback<T> callback;

		public AsyncServiceProxy(Task.Callback<T> callback, Options... options) {
			super(options);
			this.callback = callback;
		}

		@Override
		protected T invoke(Request request) throws Exception {
			BusImpl.this.invoke(request, this.callback, options);
			return null;
		}
	}

	/* ******************************************************** */

	// Routines for client, as test invoking.

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

	private Invoker<?> findInvoker(String txCode) {
		InvokerBean ivkb = BusImpl.this.router.route(txCode, BusImpl.this.config.getInvokers());
		return InvokerFactory.getInvoker(ivkb);
	}

	protected class InvokerFilter extends FilterBase implements Filter {

		/**
		 * Kernal invoking of this bus.
		 * 
		 * @param options
		 * @throws Exception
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Response execute(RequestWrapper<?> request) throws Exception {
			Options[] options = request.options();
			Request req = request.request();
			Invoker<?> ivk = findInvoker(req.code());

			before(req);

			if (null == request.callback()) {
				Response resp = ivk.invoke(req, options);
				after(resp);
				return resp;
			} else {
				ivk.invoke(req, new ResponseCallback(request.callback()), options);
				return null;
			}

		}

		private void before(Request request) {
			switch (mode) {
			case CLIENT:
				request.context(Context.serialize(Context.toMap()));
				break;
			case SERVER:
				Context.merge(Context.deserialize(request.context()));
				break;
			}
		}

		private void after(Response response) {
			switch (mode) {
			case CLIENT:
				Context.merge(Context.deserialize(response.context()));
				break;
			case SERVER:
				response.context(Context.serialize(Context.toMap()));
				break;
			}
		}

		private class ResponseCallback<R> implements Task.Callback<Response> {
			private Task.Callback<R> callback;

			public ResponseCallback(Task.Callback<R> callback) {
				super();
				this.callback = callback;
			}

			@SuppressWarnings("unchecked")
			@Override
			public void callback(Response response) throws Exception {
				after(response);
				if (response != null && this.callback != null) this.callback.callback((R) response.result());
			}
		}
	}
}
