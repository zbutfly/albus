package net.butfly.bus.support;

import java.util.concurrent.ExecutorService;

import net.butfly.albacore.utils.ExceptionUtils;
import net.butfly.albacore.utils.async.Task;

public class ContinuousUtils {
	public static void executeContinuous(final ExecutorService executor, Task<?> task) throws Throwable {
		if (task.callback() == null) throw new IllegalArgumentException("Continuous need callback.");
		ContinuousOptions copts = (ContinuousOptions) task.options();
		try {
			while (true)
				for (int i = 0; i < copts.concurrence(); i++)
					task.execute(executor);
		} catch (Throwable signal) {
			// AsyncUtils.handleSignal(signal);
		}
	}

	public static void handleSignal(Signal signal) {
		// TODO
		if (signal instanceof Signal.Error) {
			Throwable cause = ((Signal.Error) signal).getCause();
			if (cause != null) throw ExceptionUtils.wrap(cause);
		}
	}
}
