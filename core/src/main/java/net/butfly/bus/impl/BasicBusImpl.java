package net.butfly.bus.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.lambda.Consumer;
import net.butfly.albacore.utils.Keys;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.TXs;
import net.butfly.bus.config.Configuration;
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;
import net.butfly.bus.filter.FilterChain;
import net.butfly.bus.invoker.AbstractLocalInvoker;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.policy.Router;
import net.butfly.bus.utils.Constants;

abstract class BasicBusImpl implements Bus {
	private final String id;
	protected Configuration config;
	protected Router router;
	protected FilterChain chain;

	protected Mode mode;

	public BasicBusImpl(Mode mode, String conf) {
		Context.initialize(null);
		this.mode = mode;
		this.config = BusFactory.createConfiguration(conf, mode);
		this.router = BusFactory.createRouter(this.config);
		this.chain = new FilterChain(this, config.getFilterList());
		this.id = Keys.key(String.class);
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public boolean isSupported(String requestTX) {
		InvokerConfig ivkb = this.router.route(requestTX, this.config.getInvokers());
		return ivkb != null;
	}

	Method invokingMethod(TX tx) {
		Invoker ivk = this.find(tx.value());
		if (!(ivk instanceof AbstractLocalInvoker)) throw new UnsupportedOperationException(
				"Only local invokers support real method fetching by options.");
		Method m = ((AbstractLocalInvoker) ivk).getMethod(tx.value(), tx.version());
		if (null == m) throw new UnsupportedOperationException("Unsupported " + tx.toString() + "");
		return m;
	}

	protected abstract class ServiceProxy<T> implements InvocationHandler {
		protected Options[] options;

		public ServiceProxy(Options... options) {
			this.options = options;
		}

		@Override
		public Object invoke(Object obj, Method method, Object[] args) throws Exception {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				return this.invoke(request);
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method [" + method.toString()
					+ "].");
		}

		protected abstract T invoke(Request request) throws Exception;
	}

	protected final void check(final Request request) {
		if (request == null) throw new SystemException(Constants.UserError.BAD_REQUEST, "Request null invalid.");
		if (request.code() == null || "".equals(request.code().trim())) throw new SystemException(Constants.UserError.BAD_REQUEST,
				"Request empty tx code invalid.");
		if (request.version() == null) throw new SystemException(Constants.UserError.BAD_REQUEST, "Request empty tx version invalid.");
		assert null != new FlowNo(request);
		Context.txInfo(TXs.impl(request.code(), request.version()));
	}

	abstract Response invoke(Request request, Options... options) throws Exception;

	abstract void invoke(Request request, Consumer<Response> callback, Options... options) throws Exception;

	protected Invoker find(String tx) {
		// TODO: handle route failure null exception
		InvokerConfig b = router.route(tx, config.getInvokers());
		if (null == b) throw new UnsupportedOperationException("Unsupported " + tx.toString() + "");
		return b.invoker();
	}
}
