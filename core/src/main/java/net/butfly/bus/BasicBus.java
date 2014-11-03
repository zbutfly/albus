package net.butfly.bus;

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
import net.butfly.bus.argument.AsyncRequest;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.TX;
import net.butfly.bus.config.Config;
import net.butfly.bus.config.bean.invoker.InvokerBean;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;
import net.butfly.bus.facade.InternalFacade;
import net.butfly.bus.filter.Filter;
import net.butfly.bus.filter.FilterBase;
import net.butfly.bus.filter.FilterChain;
import net.butfly.bus.invoker.AbstractLocalInvoker;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.invoker.InvokerFactory;
import net.butfly.bus.invoker.ParameterInfo;
import net.butfly.bus.policy.Routeable;
import net.butfly.bus.policy.Router;
import net.butfly.bus.support.InvokeSupport;
import net.butfly.bus.util.TXUtils;
import net.butfly.bus.util.async.Signal;

public class BasicBus implements InternalFacade, Routeable, InvokeSupport {
	private static final long serialVersionUID = -4835302344711170159L;

	protected final String id;
	protected Config config;
	protected Router router;
	protected FilterChain chain;

	private String[] supportedTXs;

	/* Routine for both client and server */

	public BasicBus() {
		this(null);
	}

	public BasicBus(String configLocation) {
		this.config = BusFactory.createConfiguration(configLocation);
		this.router = BusFactory.createRouter(this.config);
		this.chain = new FilterChain(config.getFilterList(), new InvokerFilter(), this.config.side());
		this.id = this.config.id();

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
		InvokerBean ivkb = BasicBus.this.router.route(code, BasicBus.this.config.getInvokers());
		Invoker<?> ivk = InvokerFactory.getInvoker(ivkb);
		if (!(ivk instanceof AbstractLocalInvoker))
			throw new UnsupportedOperationException("Only local invokers support real method fetching by request.");
		Method m = ((AbstractLocalInvoker) ivk).getMethod(code, version);
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

	/**
	 * Kernal invoking for this bus.
	 * 
	 * @param request
	 * @return
	 */
	@Override
	public Response invoke(Request request) {
		if (request == null) throw new SystemException(Constants.UserError.BAD_REQUEST, "Request null invalid.");
		if (request.code() == null || "".equals(request.code().trim()))
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx code invalid.");
		if (request.version() == null)
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx version invalid.");
		new FlowNo(request.code(), request.version());
		try {
			return chain.execute(request);
		} catch (Signal sig) {
			throw sig;
		} catch (SystemException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SystemException("", ex);
		}
	}

	/* Routines for client */
	@Override
	@SuppressWarnings("unchecked")
	public <F extends Facade> F getService(Class<F> facadeClass) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, new ServiceProxy());
	}

	@Override
	public <T> T invoke(String code, Object... args) {
		return this.invoke(TXUtils.TXImpl(code), args);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(TX tx, Object... args) {
		Response resp = this.invoke(new Request(tx, args));
		return (T) resp.result();
	}

	protected class ServiceProxy implements InvocationHandler {
		public Object invoke(Object obj, Method method, Object[] args) {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				Response response = this.invoke(request);
				return null != response ? response.result() : null;
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method ["
					+ method.toString() + "].");
		}

		protected Response invoke(Request request) {
			return BasicBus.this.invoke(request);
		}
	}

	/* Routines for client, as test invoking. */

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

	@Override
	public boolean isDebug() {
		return false;
	}

	protected class InvokerFilter extends FilterBase implements Filter {
		@Override
		public Response execute(Request request) throws Exception {
			Response response;
			switch (this.side) {
			case CLIENT:
				request.context(Context.serialize(Context.toMap()));
				response = this.doExecute(request);
				if (null != response) Context.merge(Context.deserialize(response.context()));
				return response;
			case SERVER:
				Context.merge(Context.deserialize(request.context()));
				response = this.doExecute(request);
				if (null != response) response.context(Context.serialize(Context.toMap()));
				return response;
			}
			throw new SystemException("");
		}

		/**
		 * Kernal invoking of this bus.
		 * 
		 * @param request
		 * @return
		 */
		protected Response doExecute(Request request) {
			InvokerBean ivkb = BasicBus.this.router.route(request.code(), BasicBus.this.config.getInvokers());
			Invoker<?> ivk = InvokerFactory.getInvoker(ivkb);
			if ((request instanceof AsyncRequest) && ((AsyncRequest) request).continuous()) {
				if (!(BasicBus.this instanceof RepeatBus))
					throw new UnsupportedOperationException(
							"Only async routine supports continuous invoking, use RepeatBus.xxx(..., callback).");
				ivk.invoke(request);
				throw new IllegalAccessError("A continuous invoking should not end, invoking broken on signal or exception.");
			} else return ivk.invoke(request);
		}

	}

}
