package net.butfly.bus.ext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.AsyncUtils;
import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.support.AsyncInvokeSupport;
import net.butfly.bus.utils.TXUtils;
import net.butfly.bus.utils.async.ContinuousUtils;
import net.butfly.bus.utils.async.InvokeTask;

public class Bus extends net.butfly.bus.Bus implements AsyncInvokeSupport {
	private static final long serialVersionUID = 8122397649342253232L;

	public Bus() {
		super();
	}

	public Bus(String configLocation) {
		super(configLocation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, Callback<T> callback, Options options) {
		ServiceProxy<T> proxy = new ServiceProxy<T>(callback, options);
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, proxy);
	}

	@Override
	public <T> void invoke(String code, Object[] arguments, Callback<T> callback, Options options) {
		this.invoke(TXUtils.TXImpl(code), callback, options);
	};

	@Override
	public <T> void invoke(TX tx, Object[] arguments, Callback<T> callback, Options options) {
		this.invoke(new Request(tx, arguments), new ResponseCallback<T>(callback), options);
	}

	/**
	 * Kernal invoking of continuous bus, overlay async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into Bus.InvokerFilter for handling.
	 */
	@Override
	public <T> void invoke(Request request, Callback<T> callback, Options options) {
		Task<Response> task = new InvokeTask(new Task<Response>(new Callable<Response>() {
			@Override
			public Response call() throws Exception {
				return Bus.super.invoke(request);
			}
		}, new ResponseCallback<T>(callback), options));
		// repeated
		if (options instanceof net.butfly.bus.utils.async.Options) ContinuousUtils.execute(task);
		// async single
		else AsyncUtils.execute(task);

	}

	protected class ServiceProxy<T> extends net.butfly.bus.Bus.ServiceProxy implements InvocationHandler {
		private Options options;
		private Callback<T> callback;

		public ServiceProxy(Callback<T> callback, Options options) {
			this.callback = callback;
			this.options = options;
		}

		protected Response invoke(Request request) {
			Bus.this.invoke(request, new ResponseCallback<T>(this.callback), options);
			return null;
		}
	}

	private class ResponseCallback<R> implements Callback<Response> {
		private Callback<R> callback;

		public ResponseCallback(Callback<R> callback) {
			super();
			this.callback = callback;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void callback(Response response) {
			if (response != null) this.callback.callback((R) response.result());
			// TODO: should throw an exception?
			else logger.warn("Response null.");
		}
	}
}
