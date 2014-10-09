package net.butfly.bus;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.facade.Facade;
import net.butfly.bus.Constants.Side;
import net.butfly.bus.config.Config;
import net.butfly.bus.config.ConfigLoader;
import net.butfly.bus.config.ConfigParser;
import net.butfly.bus.config.bean.invoker.InvokerBean;
import net.butfly.bus.config.loader.ClasspathConfigLoad;
import net.butfly.bus.config.parser.XMLConfigParser;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;
import net.butfly.bus.ext.AsyncRequest;
import net.butfly.bus.ext.ContinuousBus;
import net.butfly.bus.facade.InternalFacade;
import net.butfly.bus.filter.Filter;
import net.butfly.bus.filter.FilterBase;
import net.butfly.bus.filter.FilterChain;
import net.butfly.bus.invoker.AbstractLocalInvoker;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.invoker.InvokerFactory;
import net.butfly.bus.policy.Routeable;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.RouterBase;
import net.butfly.bus.policy.SingleRouter;
import net.butfly.bus.util.TXUtils;
import net.butfly.bus.util.async.Signal;

public class Bus implements InternalFacade, Routeable, ClientFacade {
	private static final long serialVersionUID = -4835302344711170159L;

	protected final String id;
	protected final Side side;
	protected Config config;
	protected Router router;
	protected FilterChain chain;
	protected ConfigLoader loader;
	protected ConfigParser parser;

	private String[] supportedTXs;

	/* Routine for both client and server */

	public Bus() {
		this(null, Side.CLIENT);
	}

	public Bus(String configLoString) {
		this(configLoString, Side.CLIENT);
	}

	public Bus(Side side) {
		this(null, side);
	}

	public Bus(String configLocation, Side side) {
		this.side = side;
		this.initialize(configLocation);
		this.config = parser.parse();
		this.router = RouterBase.createRouter(this.config);
		if (this.router == null) this.router = new SingleRouter();
		this.chain = new FilterChain(config.getFilterList(), new InvokerFilter(), this.side);
		this.id = this.config.getBusID();

		// initialize tx supporting status
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
	public Class<?>[] getParameterTypes(String code, String version) {
		InvokerBean ivkb = Bus.this.router.route(code, Bus.this.config.getInvokers());
		Invoker<?> ivk = InvokerFactory.getInvoker(ivkb);
		if (!(ivk instanceof AbstractLocalInvoker))
			throw new UnsupportedOperationException("Only local invokers support real method fetching by request.");
		return ((AbstractLocalInvoker) ivk).getMethod(code, version).getParameterTypes();
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
	public <F extends Facade> F getService(Class<F> facadeClass) {
		return getService(facadeClass, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context) {
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

	private class ServiceProxy implements InvocationHandler {
		public Object invoke(Object obj, Method method, Object[] args) {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				Response response = Bus.this.invoke(request);
				return response.result();
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method ["
					+ method.toString() + "].");

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
			return null;
		}

		/**
		 * Kernal invoking of this bus.
		 * 
		 * @param request
		 * @return
		 */
		protected Response doExecute(Request request) {
			InvokerBean ivkb = Bus.this.router.route(request.code(), Bus.this.config.getInvokers());
			Invoker<?> ivk = InvokerFactory.getInvoker(ivkb);
			if (ivk.continuousSupported() && (request instanceof AsyncRequest) && ((AsyncRequest) request).continuous()) {
				if (!(Bus.this instanceof ContinuousBus))
					throw new UnsupportedOperationException(
							"Only async routine supports continuous invoking, use ContinuousBus.xxx(..., callback).");
				ivk.invoke(request);
				throw new IllegalAccessError("A continuous invoking should not end, invoking broken on signal or exception.");
			} else return ivk.invoke(request);
		}

	}

	/**
	 * @param configLocation
	 * 
	 * @TODO: enable multiply configuration styles.
	 * @TODO: check debug to determinate whether to load internal configuration.
	 */
	private void initialize(String configLocation) {
		if (this.side == Side.CLIENT) Context.initialize(true);
		this.loader = scanLoader(configLocation);
		this.parser = new XMLConfigParser(this.loader.load());
	}

	private ConfigLoader scanLoader(String configLocation) {
		ConfigLoader l = new ClasspathConfigLoad(configLocation);
		if (l.load() != null) return l;
		// load default
		switch (this.side) {
		case CLIENT:
			l = new ClasspathConfigLoad(Constants.Configuration.DEFAULT_CLIENT_CONFIG);
			break;
		case SERVER:
			l = new ClasspathConfigLoad(Constants.Configuration.DEFAULT_SERVER_CONFIG);
			break;
		}
		if (l.load() != null) return l;
		l = new ClasspathConfigLoad(Constants.Configuration.DEFAULT_COMMON_CONFIG);
		if (l.load() != null) return l;
		// internal config
		switch (this.side) {
		case CLIENT:
			l = new ClasspathConfigLoad(Constants.Configuration.INTERNAL_CLIENT_CONFIG);
			break;
		case SERVER:
			l = new ClasspathConfigLoad(Constants.Configuration.INTERNAL_SERVER_CONFIG);
			break;
		}
		if (l.load() != null) return l;
		l = new ClasspathConfigLoad(Constants.Configuration.INTERNAL_COMMON_CONFIG);
		if (l.load() != null) return l;
		throw new SystemException(Constants.UserError.CONFIG_ERROR, "Bus configurations invalid: " + configLocation);
	} 
}
