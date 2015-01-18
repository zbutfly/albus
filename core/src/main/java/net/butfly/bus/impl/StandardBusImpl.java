package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task.Callback;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.impl.BusFactory.Mode;
import net.butfly.bus.utils.TXUtils;

class StandardBusImpl extends BasicBusImpl {
	private static final long serialVersionUID = -4835302344711170159L;

	public StandardBusImpl(Mode mode) {
		super(mode);
	}

	public StandardBusImpl(String configLocation, Mode mode) {
		super(configLocation, mode);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, F extends Facade> F service(Class<F> facadeClass, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass },
				new StandardServiceProxy<T>(options));
	}

	/**
	 * Kernal invoking for this bus.
	 * 
	 * @param options
	 * @return
	 * @throws Exception
	 */
	@Override
	<T> Response invoke(Request request, Options... options) throws Exception {
		check(request);
		return chain.execute(request, null, options);
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

	private class StandardServiceProxy<T> extends ServiceProxy<T> {
		public StandardServiceProxy(Options... options) {
			super(options);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T invoke(Request request) throws Exception {
			return (T) StandardBusImpl.this.invoke(request, options).result();
		}
	}

	@Override
	public <T, F extends Facade> F service(Class<F> facadeClass, Callback<T> callback, Options... options) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> void invoke(String code, Object[] arguments, Callback<T> callback, Options... options) throws Exception {
		throw new UnsupportedOperationException();

	}

	@Override
	public <T> void invoke(TX tx, Object[] arguments, Callback<T> callback, Options... options) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	<R> void invoke(Request request, Callback<R> callback, Options... options) throws Exception {
		throw new UnsupportedOperationException();
	}
}
