package net.butfly.bus.impl;

import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.filter.FilterContext;
import net.butfly.bus.impl.BusFactory.Mode;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.utils.TXUtils;

abstract class StandardBusImpl extends BasicBusImpl  {
	private static final long serialVersionUID = -4835302344711170159L;

	public StandardBusImpl( Mode mode, String conf) {
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
		Invoker<InvokerConfigBean> invoker = Invokers.getInvoker(router.route(request.code(), config.getInvokers()), mode);
		return chain.execute(new FilterContext(invoker, request, null, options));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, F extends Facade> F service(Class<F> facadeClass, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass },
				new StandardServiceProxy<T>(options));
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
