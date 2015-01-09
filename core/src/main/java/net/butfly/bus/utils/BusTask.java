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
		super(task, callback, options);
		context.putAll(Context.toMap());
	}

	public BusTask(Callable<T> task, Callback<T> callback) {
		super(task, callback);
		context.putAll(Context.toMap());
	}

	public BusTask(Callable<T> task, Options options) {
		super(task, options);
		context.putAll(Context.toMap());
	}

	public BusTask(Callable<T> task) {
		super(task);
		context.putAll(Context.toMap());
	}

	public BusTask(final Task<T> original) {
		context.putAll(Context.toMap());
		this.callable = this.wrap(original.task());
		this.callback = this.wrap(original.callback());
		this.options = original.options();
	}

	private Callback<T> wrap(final Callback<T> callback) {
		return new Callback<T>() {
			@Override
			public void callback(T result) throws Exception {
				try {
					Context.initialize(context);
					if (null != callback) callback.callback(result);
					context.putAll(Context.toMap());
				} finally {
					Context.cleanup();
				}
			}
		};
	}

	private Callable<T> wrap(final Callable<T> original) {
		return new Callable<T>() {
			@Override
			public T call() throws Exception {
				Context.initialize(context);
				T r = original.call();
				context.putAll(Context.toMap());
				return r;
			}
		};
	}
}
