package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.TXes;
import net.butfly.bus.filter.FilterContext;

class BusImpl extends StandardBusImpl {
	/**
	 * Kernal invoking of back bus, async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into LastFilter for handling.
	 */
	@Override
	void invoke(final Request request, final Task.Callback<Response> callback, final Options... options) throws Exception {
		check(request);
		chain.execute(new FilterContext(find(request.code()), request, callback, mode, options));
	}

	public BusImpl(Mode mode, String conf) {
		super(mode, conf);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F service(Class<F> facadeClass, Task.Callback<T> callback, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass },
				new CallbackServiceProxy<T>(callback, options));
	}

	@Override
	public <T> void invoke(String code, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception {
		this.invoke(TXes.impl(code), arguments, callback, options);
	};

	@Override
	public <T> void invoke(TX tx, Object[] arguments, final Task.Callback<T> callback, Options... options) throws Exception {
		this.invoke(new Request(tx, arguments), new Task.Callback<Response>() {
			@SuppressWarnings("unchecked")
			@Override
			public void callback(Response result) {
				callback.callback((T) result.result());
			}
		}, options);
	}

	private class CallbackServiceProxy<T> extends ServiceProxy<T> {
		protected Task.Callback<T> callback;

		public CallbackServiceProxy(Task.Callback<T> callback, Options... options) {
			super(options);
			this.callback = callback;
		}

		@Override
		protected T invoke(Request request) throws Exception {
			BusImpl.this.invoke(request, new Task.Callback<Response>() {
				@SuppressWarnings("unchecked")
				@Override
				public void callback(Response result) {
					callback.callback((T) result.result());
				}
			}, options);
			return null;
		}
	}
}
