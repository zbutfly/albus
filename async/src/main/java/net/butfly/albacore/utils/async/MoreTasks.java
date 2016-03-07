package net.butfly.albacore.utils.async;

import java.util.concurrent.ExecutorService;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;

class MoreTasks {
	public static void executeContinuous(final ExecutorService executor, Task<?> task) throws Throwable {
		if (task.back() == null) throw new IllegalArgumentException("Continuous need back.");
		Options copts = task.options();
		try {
			while (true)
				for (int i = 0; i < copts.concurrence; i++)
					task.execute(executor);
		} catch (Throwable signal) {
			// Tasks.handleSignal(signal);
		}
	}

	// public static void handleSignal(Signal signal) {
	// // TODO
	// if (signal instanceof Signal.Error) {
	// Throwable cause = ((Signal.Error) signal).getCause();
	// if (cause != null) throw Exceptions.wrap(cause);
	// }
	// }
}
