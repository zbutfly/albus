package net.butfly.bus.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.context.Context;
<<<<<<< HEAD
import net.butfly.bus.context.Contexts;
=======
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491

public class BusTask<T> extends Task<T> {
	private final Map<String, Object> context = new ConcurrentHashMap<String, Object>();

	public BusTask(final Task<T> task) {
		context.putAll(Context.toMap());
		this.call = new Task.Callable<T>() {
			@Override
			public T call() throws Exception {
				// TODO: optimizing...
<<<<<<< HEAD
				Contexts.initialize(context);
=======
				Context.initialize(context);
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
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
<<<<<<< HEAD
				Contexts.initialize(context);
=======
				Context.initialize(context);
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
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
