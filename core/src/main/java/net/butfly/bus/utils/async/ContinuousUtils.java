package net.butfly.bus.utils.async;

import net.butfly.albacore.utils.UtilsBase;
import net.butfly.albacore.utils.async.AsyncUtils;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.albacore.utils.async.Task;

public class ContinuousUtils extends UtilsBase {
	public static <R> void execute(final Task<R> task) throws Signal {
		Options options = (Options) task.options();
		int countdown = options.retries();
		boolean uninfinite = options.retries() >= 0;
		while (uninfinite && countdown > 0) {
			try {
				for (int i = 0; i < options.concurrence(); i++)
					AsyncUtils.execute(task);
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
}
