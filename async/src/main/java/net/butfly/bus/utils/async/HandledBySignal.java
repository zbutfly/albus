package net.butfly.bus.utils.async;

import net.butfly.albacore.utils.async.Signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HandledBySignal {
	private static final Logger logger = LoggerFactory.getLogger(HandledBySignal.class);
	protected Options options;
	private int retried;

	public HandledBySignal(Options request) {
		this.options = request;
		this.retried = 0;
	}

	protected abstract void handle() throws Throwable;

	public boolean retry() {
		return ++this.retried < this.options.retries();
	}

	public static void handleBySignal(HandledBySignal handled) throws Throwable {
		boolean retry = true;
		while (retry)
			try {
				handled.handle();
				// TODO: renew retry count?
			} catch (Signal.Suspend signal) {
				logger.debug("Request is asked to be suspend for [" + signal.timeout() + "]");
				if (signal.timeout() > 0) try {
					Thread.sleep(signal.timeout());
				} catch (InterruptedException e) {}
			} catch (Signal.Timeout signal) {
				logger.debug("Request timeout, attemp to retry: [" + handled.retried + "], do retry again? [" + retry + "].");
				retry = handled.retry();
			} catch (Signal.Completed signal) {
				retry = false;
			}
	}
}
