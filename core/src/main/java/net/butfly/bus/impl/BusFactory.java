package net.butfly.bus.impl;

import net.butfly.bus.Bus;

public final class BusFactory {
	private BusFactory() {}

	public static Bus bus() {
		return bus(null);
	}

	public static Bus bus(String conf) {
		return new BusImpl(conf);
	}
}
