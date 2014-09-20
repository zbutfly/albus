package net.butfly.bus.ext;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.Constants;
import net.butfly.bus.Constants.Side;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.util.TXUtils;

/**
 * Bus client implementation for continuous invoking.
 * 
 * @author butfly
 */
public class ContinuousBus extends CallbackBus {
	private static final long serialVersionUID = 6268501157567944627L;
	private int retries = 5;

	public ContinuousBus(String configLocation) {
		super(configLocation);
	}

	public ContinuousBus() {
		super();
	}

	public ContinuousBus(Side side) {
		super(side);
	}

	public ContinuousBus(String configLocation, Side side) {
		super(configLocation, side);
	}

	public void retries(int retries) {
		this.retries = retries;
	}

	/**
	 * Kernal invoking of continuous bus, overlay async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into Bus.InvokerFilter for handling.
	 * 
	 * @param request
	 * @param callback
	 * @param retries
	 */
	// public void invoke(final AsyncRequest request) ;

	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback, int retries) {
		return this.getService(facadeClass, null, callback, retries);
	}

	@SuppressWarnings("unchecked")
	public <T, F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context,
			AsyncCallback<T> callback, int retries) {
		ServiceProxy<T> proxy = new ServiceProxy<T>(callback, retries);
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, proxy);
	}

	public <T> void invoke(String code, AsyncCallback<T> callback, int retries, Object... args) {
		this.invoke(TXUtils.TXImpl(code), callback, retries, args);
	};

	public <T> void invoke(TX tx, AsyncCallback<T> callback, int retries, Object... args) {
		this.invoke(createAsync(new Request(tx, args), new ResponseCallback<T>(callback), retries));
	}

	@Override
	public void invoke(final Request request, final AsyncCallback<Response> callback) {
		this.invoke(createAsync(request, callback, this.retries));
	}

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback) {
		return this.getService(facadeClass, callback, this.retries);
	}

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context, AsyncCallback<T> callback) {
		return this.getService(facadeClass, context, callback, this.retries);
	}

	@Override
	public <T> void invoke(String code, AsyncCallback<T> callback, Object... args) {
		this.invoke(code, callback, this.retries, args);
	};

	// TODO: overload wrong!!!
	@Override
	public <T> void invoke(TX tx, AsyncCallback<T> callback, Object... args) {
		this.invoke(tx, callback, args, this.retries, args);
	}

	private class ServiceProxy<T> implements InvocationHandler {
		private AsyncCallback<T> resultCallback;
		private int retries;

		public ServiceProxy(AsyncCallback<T> callback, int retries) {
			this.resultCallback = callback;
			this.retries = retries;
		}

		public Object invoke(Object obj, Method method, Object[] args) {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				ContinuousBus.this.invoke(createAsync(request, new ResponseCallback<T>(this.resultCallback), this.retries));
				return null;
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method ["
					+ method.toString() + "].");

		}
	}

	private AsyncRequest createAsync(final Request request, final AsyncCallback<Response> callback, int retries) {
		return new AsyncRequest(request, new AsyncCallback<Response>() {
			@Override
			public void callback(Response result) {
				ContinuousBus.this.chain.executeAfter(request, result);
				callback.callback(result);
			}
		}, true, retries);
	}
}
