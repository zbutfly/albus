package net.butfly.bus.utils.async;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import net.butfly.albacore.utils.UtilsBase;
import net.butfly.albacore.utils.async.Callable;
import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;

public final class AsyncUtils extends UtilsBase {
	public static Response execute(final Task<Response> task) throws Signal {
		if (task.options() != null && task.options() instanceof ContinuousOptions) {
			executeContinuous(null, new TaskWrapper(task));
			return null;
		} else return net.butfly.albacore.utils.async.AsyncUtils.execute(new TaskWrapper(task));
	}

	@Deprecated
	public static Response execute(final Task<Response> task, final ExecutorService executor) throws Signal {
		if (task.options() instanceof ContinuousOptions) {
			executeContinuous(executor, task);
			return null;
		} else return net.butfly.albacore.utils.async.AsyncUtils.execute(new TaskWrapper(task), executor);
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

	private static void executeContinuous(final ExecutorService executor, final Task<Response> task) throws Signal {
		if (task.callback() == null) throw new UnsupportedOperationException("Continuous need callback.");
		ContinuousOptions options = (ContinuousOptions) task.options();
		int countdown = options.retries();
		boolean uninfinite = options.retries() >= 0;
		while (uninfinite && countdown > 0) {
			try {
				for (int i = 0; i < options.concurrence(); i++)
					if (null == executor) net.butfly.albacore.utils.async.AsyncUtils.execute(task);
					else net.butfly.albacore.utils.async.AsyncUtils.execute(task, executor);
			} catch (Signal.Completed signal) {
				countdown -= options.concurrence();
			} catch (Signal.Suspend signal) {
				countdown -= options.concurrence();
				try {
					Thread.sleep(signal.timeout());
				} catch (InterruptedException e) {}
			} catch (Signal.Timeout signal) {
				countdown -= options.concurrence();
			} catch (Throwable th) {
				countdown -= options.concurrence();
			}
		}
	}

	/**
	 * @author butfly
	 *
	 *         Wrapper class for {@link net.butfly.albacore.utils.async.Task
	 *         <Response>}, support {@link net.butfly.bus.context.Context}
	 *         transporting between async threads.
	 */
	private static class TaskWrapper extends Task<Response> {
		private final Map<String, Object> context = new ConcurrentHashMap<String, Object>();

		public TaskWrapper(final Task<Response> original) {
			context.putAll(Context.toMap());
			this.task = new Callable<Response>() {
				@Override
				public Response call() throws Signal {
					Context.initialize(context);
					Response r = original.task().call();
					context.putAll(Context.toMap());
					return r;
				}
			};
			this.callback = null == original.callback() ? null : new Callback<Response>() {
				@Override
				public void callback(Response result) throws Signal {
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
