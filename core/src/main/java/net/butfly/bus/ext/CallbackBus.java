package net.butfly.bus.ext;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.Bus;
import net.butfly.bus.Constants;
import net.butfly.bus.Constants.Side;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.util.TXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bus client implementation for callback invoking.
 * 
 * @author butfly
 */
public class CallbackBus extends Bus implements CallbackClientFacade {
	private static final long serialVersionUID = 2146028887826838753L;
	private static final Logger logger = LoggerFactory.getLogger(CallbackBus.class);

	public CallbackBus() {
		super();
	}

	public CallbackBus(String configLocation) {
		super(configLocation);
	}

	public CallbackBus(Side side) {
		super(side);
	}

	public CallbackBus(String configLocation, Side side) {
		super(configLocation, side);
	}

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback) {
		return this.getService(facadeClass, null, callback);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context, AsyncCallback<T> callback) {
		ServiceProxy<T> proxy = new ServiceProxy<T>(callback);
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, proxy);
	}

	@Override
	public <T> void invoke(String code, AsyncCallback<T> callback, Object... args) {
		this.invoke(TXUtils.TXImpl(code), callback, args);
	};

	@Override
	public <T> void invoke(TX tx, AsyncCallback<T> callback, Object... args) {
		this.invoke(new Request(tx, args), new ResponseCallback<T>(callback));
	}

	/**
	 * Kernal invoking of async bus.
	 * 
	 * @param request
	 * @param callback
	 * @param timeout
	 */
	@Override
	public void invoke(final Request request, final AsyncCallback<Response> callback) {
		callback.callback(CallbackBus.super.invoke(request));
	}

	protected class ResponseCallback<T> implements AsyncCallback<Response> {
		private AsyncCallback<T> resultCallback;

		public ResponseCallback(AsyncCallback<T> resultCallback) {
			super();
			this.resultCallback = resultCallback;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void callback(Response response) {
			if (response != null) this.resultCallback.callback((T) response.result());
			else logger.warn("Response null.");
		}
	}

	private class ServiceProxy<T> implements InvocationHandler {
		private AsyncCallback<T> resultCallback;

		public ServiceProxy(AsyncCallback<T> callback) {
			this.resultCallback = callback;
		}

		public Object invoke(Object obj, Method method, Object[] args) {
			TX tx = method.getAnnotation(TX.class);
			if (null != tx) {
				Request request = new Request(tx.value(), tx.version(), args);
				CallbackBus.this.invoke(request, new ResponseCallback<T>(this.resultCallback));
				return null;
			} else throw new SystemException(Constants.UserError.TX_NOT_EXIST, "Request tx code not found on method ["
					+ method.toString() + "].");

		}
	}
}
