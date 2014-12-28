package net.butfly.bus.utils.async;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import net.butfly.albacore.utils.UtilsBase;
import net.butfly.albacore.utils.async.Callable;
import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.context.Context;

public final class AsyncUtils extends UtilsBase {
	public static <R> R execute(final Task<R> task) throws Signal {
		if (task.options() != null && task.options() instanceof ContinuousOptions) {
			executeContinuous(null, new TaskWrapper<R>(task));
			return null;
		} else return net.butfly.albacore.utils.async.AsyncUtils.execute(new TaskWrapper<R>(task));
	}

	public static <R> R execute(final Task<R> task, final ExecutorService executor) throws Signal {
		if (task.options() instanceof ContinuousOptions) {
			executeContinuous(executor, task);
			return null;
		} else return net.butfly.albacore.utils.async.AsyncUtils.execute(new TaskWrapper<R>(task), executor);
	}

	public static void handleSignal(Signal signal) throws Signal {
		// TODO
		if (signal instanceof Signal.Completed) {
			Throwable cause = ((Signal.Completed) signal).getCause();
			if (cause != null) {
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;
				else throw new RuntimeException("Operation completed by signal.", cause);
			} else return;
		} else throw signal;
	}

	private static <R> void executeContinuous(final ExecutorService executor, final Task<R> task) throws Signal {
		if (task.callback() == null) throw new IllegalArgumentException("Continuous need callback.");
		ContinuousOptions copts = (ContinuousOptions) task.options();
		try {
			while (true)
				for (int i = 0; i < copts.concurrence(); i++)
					net.butfly.albacore.utils.async.AsyncUtils.execute(task, executor);
		} catch (Signal signal) {
			AsyncUtils.handleSignal(signal);
		}
	}

	/**
	 * @author butfly
	 *
	 *         Wrapper class for {@link net.butfly.albacore.utils.async.Task
	 *         <Response>}, support {@link net.butfly.bus.context.Context}
	 *         transporting between async threads.
	 */
	private static class TaskWrapper<R> extends Task<R> {
		private final Map<String, Object> context = new ConcurrentHashMap<String, Object>();

		public TaskWrapper(final Task<R> original) {
			context.putAll(Context.toMap());
			this.task = new Callable<R>() {
				@Override
				public R call() throws Signal {
					Context.initialize(context);
					R r = original.task().call();
					context.putAll(Context.toMap());
					return r;
				}
			};
			this.callback = null == original.callback() ? null : new Callback<R>() {
				@Override
				public void callback(R result) throws Signal {
					try {
						Context.initialize(context);
						original.callback().callback(result);
						context.putAll(Context.toMap());
					} finally {
						Context.cleanup();
					}
				}
			};
			this.options = original.options();
		}
	}
}
