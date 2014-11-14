package net.butfly.bus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.albacore.utils.AsyncUtils;
import net.butfly.bus.argument.AsyncRequest;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.TX;
import net.butfly.bus.context.Context;
import net.butfly.bus.support.RepeatInvokeSupport;
import net.butfly.bus.util.TXUtils;
import net.butfly.bus.util.async.AsyncInvokeUtils;
import net.butfly.bus.util.async.AsyncInvokeUtils.InvokeOption;
import net.butfly.bus.util.async.AsyncInvokeUtils.InvokeTask;
import net.butfly.bus.util.async.AsyncInvokeUtils.InvokeTaskCallback;

/**
 * Bus client implementation for continuous invoking.
 * 
 * @author butfly
 */
public class RepeatBus extends AsyncBus implements RepeatInvokeSupport {
	private static final long serialVersionUID = 6268501157567944627L;
	private int retries = -1;

	public RepeatBus(String configLocation) {
		super(configLocation);
	}

	public RepeatBus() {
		super();
	}

	public void retries(int retries) {
		this.retries = retries;
	}

	@SuppressWarnings("unchecked")
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback, long timeout, int retries) {
		ServiceProxy<T> proxy = new ServiceProxy<T>(callback, this.timeout, retries);
		return (F) Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[] { facadeClass }, proxy);
	}

	public <T> void invoke(String code, AsyncCallback<T> callback, long timeout, int retries, Object... args) {
		this.invoke(TXUtils.TXImpl(code), callback, retries, args);
	};

	public <T> void invoke(TX tx, AsyncCallback<T> callback, long timeout, int retries, Object... args) {
		this.invoke(createAsync(new Request(tx, args), new ResponseCallback<T>(callback), retries));
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
	@Override
	public void invoke(Request request, AsyncCallback<Response> callback, long timeout, int retries) {
		// produce in another thread, consume here.
		AsyncInvokeUtils.invoke(new AsyncTask<Response>(callback) {
			@Override
			public Response call() throws Exception {
				return RepeatBus.super.invoke(request);
			}
		}, new InvokeOption(timeout), new InvokeOption(), retries);
	}

	@Override
	public void invoke(final Request request, final AsyncCallback<Response> callback) {
		this.invoke(createAsync(request, callback, retries));
	}

	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback) {
		return this.getService(facadeClass, callback, retries);
	}

	@Override
	public <T> void invoke(String code, AsyncCallback<T> callback, Object... args) {
		this.invoke(code, callback, retries, args);
	};

	@Override
	public <T> void invoke(TX tx, AsyncCallback<T> callback, Object... args) {
		this.invoke(tx, callback, retries, args);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback, long timeout) {
		ServiceProxy<T> proxy = new ServiceProxy<T>(callback, timeout, retries);
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
				return RepeatBus.super.invoke(request);
			}
		}, callback, Context.toMap()), 0);
	}

	protected class ServiceProxy<T> extends AsyncBus.ServiceProxy<T> implements InvocationHandler {
		private int retries;

		public ServiceProxy(AsyncCallback<T> callback, long timeout, int retries) {
			super(callback, timeout);
			this.retries = retries;
		}

		protected Response invoke(Request request) {
			RepeatBus.this.invoke(request, new ResponseCallback<T>(this.resultCallback), this.timeout, this.retries);
			return null;
		}
	}

	private AsyncRequest createAsync(final Request request, final AsyncCallback<Response> callback, int retries) {
		AsyncCallback<Response> cb = new AsyncCallback<Response>() {
			@Override
			public void callback(Response result) {
				RepeatBus.this.chain.executeAfter(request, result);
				callback.callback(result);
			}
		};
		return new AsyncRequest(request, cb, retries);
	}
}
