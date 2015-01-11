package net.butfly.bus.support;

import net.butfly.albacore.utils.async.Options;

public class ContinuousOptions extends Options {
	public static final int RETRIES_MAX = 100;
	private int repeat = 0; // no repeat, <0 for infinity, >0 for repeat times.
	private int retries = RETRIES_MAX;
	private int concurrence = 1;

	public boolean continu(boolean successfully) {
		if (repeat == 0) return false; // not continuous operation.
		if (!successfully) retries--;
		if (retries <= 0) return false;
		if (repeat < 0) return true;
		repeat--;
		return repeat > 0;
	}

	public int repeat() {
		return repeat;
	}

	public ContinuousOptions repeat(int repeat) {
		this.repeat = repeat;
		return this;
	}

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
