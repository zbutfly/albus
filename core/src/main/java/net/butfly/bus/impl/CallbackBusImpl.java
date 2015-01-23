package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.filter.FilterContext;
import net.butfly.bus.impl.BusFactory.Mode;
import net.butfly.bus.utils.TXUtils;

public class CallbackBusImpl extends StandardBusImpl implements CallbackBus {
	private static final long serialVersionUID = -4952475921832979927L;

	public CallbackBusImpl(Mode mode) {
		super(mode);
	}

	public CallbackBusImpl(String configLocation, Mode mode) {
		super(configLocation, mode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F service(Class<F> facadeClass, Task.Callback<T> callback, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass },
				new CallbackServiceProxy<T>(callback, options));
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
	 * Kernal invoking of back bus, async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into LastFilter for handling.
	 */
	@Override
	<R> void invoke(final Request request, final Task.Callback<R> callback, final Options... options) throws Exception {
		check(request);
		chain.execute(new FilterContext(Invokers.getInvoker(router.route(request.code(), config.getInvokers()), mode), request,
				new Task.Callback<Response>() {
					@SuppressWarnings("unchecked")
					@Override
					public void callback(Response response) {
						if (response.error() == null && null != callback) callback.callback((R) response.result());
					}
				}, options));
	}

	private class CallbackServiceProxy<T> extends ServiceProxy<T> {
		protected Task.Callback<T> callback;

		public CallbackServiceProxy(Task.Callback<T> callback, Options... options) {
			super(options);
			this.callback = callback;
		}

		@Override
		protected T invoke(Request request) throws Exception {
			CallbackBusImpl.this.invoke(request, this.callback, options);
			return null;
		}
	}
}
