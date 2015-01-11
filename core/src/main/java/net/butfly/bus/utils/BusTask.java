package net.butfly.bus.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.context.Context;

public class BusTask<T> extends Task<T> {
	private final Map<String, Object> context = new ConcurrentHashMap<String, Object>();

	public BusTask() {
		super();
		context.putAll(Context.toMap());
	}

	public BusTask(Callable<T> task, Callback<T> callback, Options options) {
		context.putAll(Context.toMap());
		this.callable = new BusCallable(task);
		this.callback = null == callback ? null : new BusCallback(callback);
		this.options = options;
	}

	public BusTask(Callable<T> task, Callback<T> callback) {
		this(task, callback, null);
	}

	public BusTask(Callable<T> task, Options options) {
		this(task, null, options);
	}

	public BusTask(Callable<T> task) {
		this(task, null, null);
	}

	// public static <T> BusTask<T> wrap(final Task<T> original) {
	// return new BusTask<T>(original.task(), original.callback(), original.options());
	// }

	private class BusCallback implements Task.Callback<T> {
		Task.Callback<T> callback;

		public BusCallback(Task.Callback<T> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void callback(T result) throws Exception {
			// TODO: optimizing...
			Context.initialize(context);
			if (null != callback) callback.callback(result);
			context.putAll(Context.toMap());
		}
	}

	private class BusCallable implements Task.Callable<T> {
		Task.Callable<T> callable;

		public BusCallable(Task.Callable<T> callable) {
			super();
			this.callable = callable;
		}

		@Override
		public T call() throws Exception {
			// TODO: optimizing...
			Context.initialize(context);
			T r = callable.call();
			context.putAll(Context.toMap());
			return r;
		}
	}
}
