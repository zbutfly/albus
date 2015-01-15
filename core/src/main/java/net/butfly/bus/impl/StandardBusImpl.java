package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.StandardBus;
import net.butfly.bus.TX;
import net.butfly.bus.utils.RequestWrapper;
import net.butfly.bus.utils.TXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Signal;

class StandardBusImpl extends BusBase implements StandardBus {
	private static final long serialVersionUID = -4835302344711170159L;
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

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
	 * @throws Signal
	 */
	public <T> Response invoke(Request request, Options... options) throws Exception {
		check(request);
		return chain.execute(new RequestWrapper<T>(request, options));
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
}
