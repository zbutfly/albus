package net.butfly.bus.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.context.Context;

public class BusTask<T> extends Task<T> {
	private final Map<String, Object> context = new ConcurrentHashMap<String, Object>();

	public BusTask(Task<T> task) {
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
			public void callback(T result) throws Exception {
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
		this.exception(new Task.ExceptionHandler<T>() {
			@Override
			public T handle(Exception exception) throws Exception {
				return task.call().handle(exception);
			}
		}, HandlerTarget.CALLABLE);
		if (back != null) this.exception(new Task.ExceptionHandler<T>() {
			@Override
			public T handle(Exception exception) throws Exception {
				return task.back().handle(exception);
			}
		}, HandlerTarget.CALLBACK);
	}

	public BusTask<T> exception(Task.ExceptionHandler<T> handler, HandlerTarget... targets) {
		return (BusTask<T>) wrapHandler(this, new Task.ExceptionHandler<T>() {
			@Override
			public T handle(Exception exception) throws Exception {
				Context.initialize(context);
				try {
					return handler.handle(exception);
				} finally {
					context.putAll(Context.toMap());
				}
			}
		}, targets);
	}
}
