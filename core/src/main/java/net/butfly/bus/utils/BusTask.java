package net.butfly.bus.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.context.Context;

public class BusTask<T> extends Task<T> {
	private final Map<String, Object> context = new ConcurrentHashMap<String, Object>();

	public BusTask(final Task<T> task) {
		context.putAll(Context.toMap());
		this.call = new Task.Callable<T>() {
			@Override
			public T call() throws Exception {
				// TODO: optimizing...
				Context.initialize(context);
				try {
					return task.call().call();
				} finally {
					context.putAll(Context.toMap());
				}
			}
		};
		this.back = null == task.back() ? null : new Task.Callback<T>() {
			@Override
			public void callback(T result) {
				// TODO: optimizing...
				Context.initialize(context);
				try {
					task.back().callback(result);
				} finally {
					context.putAll(Context.toMap());
				}
			}
		};
		this.options = task.options();
		this.handler = task.handler();
	}
}
