package net.butfly.bus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.albacore.utils.AsyncUtils;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.TX;
import net.butfly.bus.context.Context;
import net.butfly.bus.support.TimeoutInvokeSupport;
import net.butfly.bus.util.TXUtils;
import net.butfly.bus.util.async.AsyncInvokeUtils.InvokeTask;
import net.butfly.bus.util.async.AsyncInvokeUtils.InvokeTaskCallback;

/**
 * Bus client implementation for async invoking.
 * 
 * @author butfly
 */
public class AsyncBus extends CallbackBus implements TimeoutInvokeSupport {
	private static final long serialVersionUID = -6481151237811258237L;
	protected long timeout = -1;

	public AsyncBus(String configLocation) {
		super(configLocation);
	}

	public AsyncBus() {
		super();
	}

	public void timeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback) {
		return this.getService(facadeClass, callback, this.timeout);
	}

	@Override
	public void invoke(Request request, AsyncCallback<Response> callback) {
		this.invoke(request, callback, this.timeout);
	}

	@Override
	public <T> void invoke(String code, AsyncCallback<T> callback, Object... args) {
		this.invoke(code, callback, this.timeout, args);
	}

	@Override
	public <T> void invoke(TX tx, AsyncCallback<T> callback, Object... args) {
		this.invoke(tx, callback, this.timeout, args);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback, long timeout) {
		ServiceProxy<T> proxy = new ServiceProxy<T>(callback, timeout);
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, proxy);
	}

	@Override
	public <T> void invoke(String code, AsyncCallback<T> callback, long timeout, Object... args) {
		this.invoke(TXUtils.TXImpl(code), callback, timeout, args);
	};

	@Override
	public <T> void invoke(TX tx, AsyncCallback<T> callback, long timeout, Object... args) {
		this.invoke(new Request(tx, args), new ResponseCallback<T>(callback), timeout);
	}

	/**
	 * Kernal invoking of async bus.
	 * 
	 * @param request
	 * @param callback
	 * @param timeout
	 */
	@Override
	public void invoke(final Request request, final AsyncCallback<Response> callback, final long timeout) {
		AsyncUtils.invoke(new InvokeTask(new InvokeTaskCallback() {
			@Override
			public Response invoke() {
				return AsyncBus.super.invoke(request);
			}
		}, callback, Context.CURRENT), timeout);
	}

	protected class ServiceProxy<T> extends CallbackBus.ServiceProxy<T> implements InvocationHandler {
		protected long timeout;

		public ServiceProxy(AsyncCallback<T> callback, long timeout) {
			super(callback);
			this.timeout = timeout;
		}

		protected Response invoke(Request request) {
			AsyncBus.this.invoke(request, new ResponseCallback<T>(this.resultCallback), this.timeout);
			return null;
		}
	}
}
