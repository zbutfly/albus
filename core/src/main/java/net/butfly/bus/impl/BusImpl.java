package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Bus;
import net.butfly.bus.Buses;
import net.butfly.bus.Mode;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.TXs;
import net.butfly.bus.filter.FilterContext;

class BusImpl extends BaseBus implements Bus {
	public BusImpl(Mode mode, String conf) {
		super(mode, conf);
	}

	/**
	 * Kernal invoking for this bus.
	 * 
	 * @param options
	 * @return
	 * @throws Exception
	 */
	@Override
	Response invoke(Request request, Options... options) throws Exception {
		check(request);
		return chain.execute(new FilterContext(find(request.code()), request, null, mode, options));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, F extends Facade> F service(Class<F> facadeClass, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass },
				new StandardServiceProxy<T>(options));
	}

	@Override
	public <T> T invoke(String code, Object[] args, Options... options) throws Exception {
		return this.invoke(TXs.impl(code), args, options);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(TX tx, Object[] args, Options... options) throws Exception {
		Response resp = this.invoke(Buses.request(tx, args), options);
		return (T) resp.result();
	}

	private class StandardServiceProxy<T> extends ServiceProxy<T> {
		public StandardServiceProxy(Options... options) {
			super(options);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T invoke(Request request) throws Exception {
			return (T) BusImpl.this.invoke(request, options).result();
		}
	}
}
