package net.butfly.bus.ext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Callable;
import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.support.InvokeExSupport;
import net.butfly.bus.utils.TXUtils;
import net.butfly.bus.utils.async.AsyncUtils;

public class Bus extends net.butfly.bus.Bus implements InvokeExSupport {
	private static final long serialVersionUID = 8122397649342253232L;

	public Bus() {
		super();
	}

	public Bus(String configLocation) {
		super(configLocation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, Callback<T> callback, Options... options) {
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, new ServiceProxy<T>(
				callback, options));
	}

	@Override
	public <T> void invoke(String code, Object[] arguments, Callback<T> callback, Options... options) throws Signal {
		this.invoke(TXUtils.TXImpl(code), arguments, callback, options);
	};

	@Override
	public <T> void invoke(TX tx, Object[] arguments, Callback<T> callback, Options... options) throws Signal {
		this.invoke(new Request(tx, arguments), callback, options);
	}

	/**
	 * Kernal invoking of continuous bus, overlay async bus. <br>
	 * Does not start async here, <br>
	 * but transfer it into Bus.InvokerFilter for handling.
	 */
	@Override
	public <T> void invoke(final Request request, Callback<T> callback, Options... options) throws Signal {
		final Options[] opts = normalize(options);
		Task<Response> task = new Task<Response>(new Callable<Response>() {
			@Override
			public Response call() throws Signal {
				return Bus.super.invoke(request, opts[1]);
			}
		}, new ResponseCallback<T>(callback), opts[0]);
		try {
			// async single
			AsyncUtils.execute(task);
		} catch (Signal signal) {
			AsyncUtils.handleSignal(signal);
		}
	}

	protected class ServiceProxy<T> extends net.butfly.bus.Bus.ServiceProxy implements InvocationHandler {
		protected Callback<T> callback;

		public ServiceProxy(Callback<T> callback, Options... options) {
			super(options);
			this.callback = callback;
		}

		protected Response invoke(Request request) throws Signal {
			Bus.this.invoke(request, this.callback, options);
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
		public void callback(Response response) throws Signal {
			if (response != null) {
				R result = (R) response.result();
				this.callback.callback(result);
			}
			// TODO: should throw an exception?
			else logger.warn("Response null.");
		}
	}

	private Options[] normalize(Options... options) {
		if (options == null || options.length == 0) return new Options[] { null, null };
		else if (options.length == 1) return new Options[] { null, options[0] };
		else return options;
	}
}
