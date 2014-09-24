package net.butfly.bus.ext;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.albacore.utils.AsyncUtils;
import net.butfly.bus.Constants;
import net.butfly.bus.Constants.Side;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.context.Context;
import net.butfly.bus.util.TXUtils;
import net.butfly.bus.util.async.AsyncInvokeUtils.InvokeTask;
import net.butfly.bus.util.async.AsyncInvokeUtils.InvokeTaskCallback;

/**
 * Bus client implementation for async invoking.
 * 
 * @author butfly
 */
public class AsyncBus extends CallbackBus implements TimeoutClientFacade {
	private static final long serialVersionUID = -6481151237811258237L;
	protected long timeout = 0;

	public AsyncBus(String configLocation) {
		super(configLocation);
	}

	public AsyncBus() {
		super();
	}

	public AsyncBus(Side side) {
		super(side);
	}

	public AsyncBus(String configLocation, Side side) {
		super(configLocation, side);
	}

	public void timeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback) {
		return this.getService(facadeClass, null, callback, this.timeout);
	}

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context, AsyncCallback<T> callback) {
		return this.getService(facadeClass, context, callback, this.timeout);
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

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback, long timeout) {
		return getService(facadeClass, null, callback);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context,
			AsyncCallback<T> callback, long timeout) {
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
		}, callback, Context.CURRENT), 0);
	}

	private class ServiceProxy<T> implements InvocationHandler {
		private AsyncCallback<T> resultCallback;
		private long timeout;

		public ServiceProxy(AsyncCallback<T> callback, long timeout) {
			this.resultCallback = callback;
			this.timeout = timeout;
		}

		public Object invoke(Object obj, Method method, Object[] args) {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				AsyncBus.this.invoke(request, new ResponseCallback<T>(this.resultCallback), this.timeout);
				return null;
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method ["
					+ method.toString() + "].");

		}
	}
}
