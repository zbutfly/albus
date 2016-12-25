package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.lambda.Consumer;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.AsyncBus;
import net.butfly.bus.Buses;
import net.butfly.bus.Mode;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.TXs;
import net.butfly.bus.filter.FilterContext;

class AsyncBusImpl extends BusImpl implements AsyncBus {
	/**
	 * Kernal invoking of back bus, async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into LastFilter for handling.
	 */
	void invoke(final Request request, final Consumer<Response> callback, final Options... options) throws Exception {
		check(request);
		chain.execute(new FilterContext(find(request.code()), request, callback, mode, options));
	}

	public AsyncBusImpl(Mode mode, String conf) {
		super(mode, conf);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F> F service(Class<F> facadeClass, Consumer<T> callback, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, new CallbackServiceProxy<T>(
				callback, options));
	}

	@Override
	public <T> void invoke(String code, Object[] arguments, Consumer<T> callback, Options... options) throws Exception {
		this.invoke(TXs.impl(code), arguments, callback, options);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> void invoke(TX tx, Object[] arguments, final Consumer<T> callback, Options... options) throws Exception {
		this.invoke(Buses.request(tx, arguments), result -> callback.accept((T) result.result()), options);
	}

	private class CallbackServiceProxy<T> extends ServiceProxy<T> {
		protected Consumer<T> callback;

		public CallbackServiceProxy(Consumer<T> callback, Options... options) {
			super(options);
			this.callback = callback;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected T invoke(Request request) throws Exception {
			AsyncBusImpl.this.invoke(request, result -> callback.accept((T) result.result()), options);
			return null;
		}
	}
}
