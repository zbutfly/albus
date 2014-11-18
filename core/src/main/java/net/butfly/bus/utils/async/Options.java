package net.butfly.bus.utils.async;

public class Options extends net.butfly.albacore.utils.async.Options {
	public static final int RETRIES_MAX = 100;
	private int retries = RETRIES_MAX;
	private int concurrence = 1;

	public Options retries(int retries) {
		this.retries = retries <= 0 || retries > RETRIES_MAX ? RETRIES_MAX : retries;
		return this;
	}

	int retries() {
		return retries;
	}

	public Options concurrence(int concurrence) {
		this.concurrence = concurrence;
		return this;
	}

	int concurrence() {
		return concurrence;
	}
}
