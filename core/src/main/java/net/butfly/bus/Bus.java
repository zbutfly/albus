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
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.argument.Constants;
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
import net.butfly.bus.support.InvokeSupport;
import net.butfly.bus.utils.BusFactory;
import net.butfly.bus.utils.TXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bus implements InternalFacade, Routeable, InvokeSupport {
	private static final long serialVersionUID = -4835302344711170159L;
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final String id;
	protected Config config;
	protected Router router;
	protected FilterChain chain;

	private String[] supportedTXs;

	/* Routine for both client and server */

	public Bus() {
		this(null);
	}

	public Bus(String configLocation) {
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
		InvokerBean ivkb = Bus.this.router.route(code, Bus.this.config.getInvokers());
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

	/**
	 * Kernal invoking for this bus.
	 * 
	 * @param options
	 * @return
	 * @throws Signal
	 */
	@Override
	public Response invoke(Request request, Options... options) throws Signal {
		if (request == null) throw new SystemException(Constants.UserError.BAD_REQUEST, "Request null invalid.");
		if (request.code() == null || "".equals(request.code().trim()))
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx code invalid.");
		if (request.version() == null)
			throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx version invalid.");
		new FlowNo(request);
		Context.txInfo(TXUtils.TXImpl(request.code(), request.version()));
		return chain.execute(new RequestAsyncWrapper(request, options));
	}

	/* Routines for client */
	@Override
	@SuppressWarnings("unchecked")
	public <F extends Facade> F getService(Class<F> facadeClass, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, new ServiceProxy(
				options));
	}

	@Override
	public <T> T invoke(String code, Object[] args, Options... options) throws Signal {
		return this.invoke(TXUtils.TXImpl(code), args, options);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(TX tx, Object[] args, Options... options) throws Signal {
		Response resp = this.invoke(new Request(tx, args), options);
		return (T) resp.result();
	}

	protected class ServiceProxy implements InvocationHandler {
		protected Options[] options;

		public ServiceProxy(Options... options) {
			this.options = options;
		}

		public Object invoke(Object obj, Method method, Object[] args) throws Signal {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				Response response = this.invoke(request);
				return null != response ? response.result() : null;
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method ["
					+ method.toString() + "].");
		}

		protected Response invoke(Request request) throws Signal {
			return Bus.this.invoke(request, options);
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

	protected class InvokerFilter extends FilterBase implements Filter {
		@Override
		public Response execute(Request request) throws Signal {
			Response response;
			Options options = null;
			if (request instanceof RequestAsyncWrapper) {
				Options[] opts = ((RequestAsyncWrapper) request).options();
				if (opts != null && opts.length > 0) options = opts[0];
				// unwrap request.
				request = new Request(request.id(), request.code(), request.version(), request.context(), request.arguments());
			}
			switch (this.side) {
			case CLIENT:
				request.context(Context.serialize(Context.toMap()));
				response = realInvoke(request, options);
				if (null != response) Context.merge(Context.deserialize(response.context()));
				return response;
			case SERVER:
				Context.merge(Context.deserialize(request.context()));
				response = realInvoke(request, options);
				if (null != response) response.context(Context.serialize(Context.toMap()));
				return response;
			}
			throw new SystemException("");
		}
	}

	/**
	 * Kernal invoking of this bus.
	 * 
	 * @param options
	 */
	private Response realInvoke(Request request, Options options) throws Signal {
		Invoker<?> ivk = this.findInvoker(request.code());
		return ivk.invoke(request, options);
	}

	private Invoker<?> findInvoker(String txCode) {
		InvokerBean ivkb = Bus.this.router.route(txCode, Bus.this.config.getInvokers());
		return InvokerFactory.getInvoker(ivkb);
	}

	private static class RequestAsyncWrapper extends Request {
		private static final long serialVersionUID = 7284007663713259222L;
		private Options[] options;

		public RequestAsyncWrapper(Request request, Options... options) {
			super(request.id(), request.code(), request.version(), request.context(), request.arguments());
			this.options = options;
		}

		public Options[] options() {
			return this.options;
		}
	}
}
