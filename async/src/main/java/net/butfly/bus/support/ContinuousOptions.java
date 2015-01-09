package net.butfly.bus.support;

import net.butfly.albacore.utils.async.Options;

public class ContinuousOptions extends Options {
	public static final int RETRIES_MAX = 100;
	private int retries = RETRIES_MAX;
	private int concurrence = 1;

	public ContinuousOptions retries(int retries) {
		this.retries = retries <= 0 || retries > RETRIES_MAX ? RETRIES_MAX : retries;
		return this;
	}

	public int retries() {
		return retries;
	}

	public ContinuousOptions concurrence(int concurrence) {
		this.concurrence = concurrence;
		return this;
	}

	int concurrence() {
		return concurrence;
	}
}
