package net.butfly.bus.ext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.AsyncUtils;
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
import net.butfly.bus.utils.async.ContinuousOptions;
import net.butfly.bus.utils.async.ContinuousUtils;
import net.butfly.bus.utils.async.InvokeTask;
import net.butfly.bus.utils.async.RequestWrapper;

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
		ServiceProxy<T> proxy = new ServiceProxy<T>(callback, options);
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, proxy);
	}

	@Override
	public <T> void invoke(String code, Object[] arguments, Callback<T> callback, Options... options) throws Signal {
		this.invoke(TXUtils.TXImpl(code), callback, options);
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
	public <T> void invoke(Request request, Callback<T> callback, Options... options) throws Signal {
		final Options[] opts = normalize(options);
		Task<Response> task = new InvokeTask(new Task<Response>(new Callable<Response>() {
			@Override
			public Response call() throws Signal {
				return Bus.super.invoke(new RequestWrapper(request, opts[0]));
			}
		}, new ResponseCallback<T>(callback), opts[1]));

		if (opts[0] instanceof ContinuousOptions) try {
			// repeated
			ContinuousUtils.execute(task);
		} catch (Signal signal) {
			AsyncUtils.handleSignal(signal);
		}
		else try {
			// async single
			AsyncUtils.execute(task);
		} catch (Signal signal) {
			AsyncUtils.handleSignal(signal);
		}
	}

	protected class ServiceProxy<T> extends net.butfly.bus.Bus.ServiceProxy implements InvocationHandler {
		private Options[] options;
		private Callback<T> callback;

		public ServiceProxy(Callback<T> callback, Options... options) {
			this.callback = callback;
			this.options = normalize(options);
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

	private Options[] normalize(Options[] options) {
		if (options == null || options.length == 0) return new Options[] { new Options(), new Options() };
		if (options.length == 1) return new Options[] { options[0], new Options() };
		return options;
	}
}
