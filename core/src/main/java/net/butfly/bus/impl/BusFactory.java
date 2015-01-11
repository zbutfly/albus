package net.butfly.bus.impl;

import net.butfly.bus.Bus;

public final class BusFactory {
	private BusFactory() {}

	public static Bus bus() {
		return bus(BusMode.CLIENT);
	}

	public static Bus bus(String conf) {
		return bus(conf, BusMode.CLIENT);
	}

	public static Bus bus(BusMode mode) {
		return bus(null, mode);
	}

	public static Bus bus(String conf, BusMode mode) {
		return new BusImpl(conf, mode);
	}
}
