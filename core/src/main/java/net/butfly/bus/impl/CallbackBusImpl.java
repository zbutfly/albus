package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.Request;
import net.butfly.bus.TX;
import net.butfly.bus.utils.RequestWrapper;
import net.butfly.bus.utils.TXUtils;

public class CallbackBusImpl extends AbstractBusImpl implements CallbackBus {
	private static final long serialVersionUID = -4952475921832979927L;

	public CallbackBusImpl(BusMode mode) {
		super(mode);
	}

	public CallbackBusImpl(String configLocation, BusMode mode) {
		super(configLocation, mode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F service(Class<F> facadeClass, Task.Callback<T> callback, Options... options) {
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
	 * Kernal invoking of back bus, async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into InvokerFilter for handling.
	 */
	public <T> void invoke(final Request request, Task.Callback<T> callback, final Options... options) throws Exception {
		super.check(request);
		chain.execute(new RequestWrapper<T>(request, callback, options));
	}

	private class AsyncServiceProxy<T> extends ServiceProxy<T> {
		protected Task.Callback<T> callback;

		public AsyncServiceProxy(Task.Callback<T> callback, Options... options) {
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
