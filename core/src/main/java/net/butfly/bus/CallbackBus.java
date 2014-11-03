package net.butfly.bus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.TX;
import net.butfly.bus.support.CallbackInvokeSupport;
import net.butfly.bus.util.TXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bus client implementation for callback invoking.
 * 
 * @author butfly
 */
public class CallbackBus extends BasicBus implements CallbackInvokeSupport {
	private static final long serialVersionUID = 2146028887826838753L;
	private static final Logger logger = LoggerFactory.getLogger(CallbackBus.class);

	public CallbackBus() {
		super();
	}

	public CallbackBus(String configLocation) {
		super(configLocation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback) {
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
			// TODO: should throw an exception?
			else logger.warn("Response null.");
		}
	}

	protected class ServiceProxy<T> extends BasicBus.ServiceProxy implements InvocationHandler {
		protected AsyncCallback<T> resultCallback;

		public ServiceProxy(AsyncCallback<T> callback) {
			this.resultCallback = callback;
		}

		protected Response invoke(Request request) {
			CallbackBus.this.invoke(request, new ResponseCallback<T>(this.resultCallback));
			return null;
		}
	}
}
