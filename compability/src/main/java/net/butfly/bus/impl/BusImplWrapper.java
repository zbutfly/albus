package net.butfly.bus.impl;

import net.butfly.bus.StandardBus;

public class BusImplWrapper extends StandardBusImpl implements StandardBus {
	private static final long serialVersionUID = -8836827099838805984L;

	public BusImplWrapper(BusMode mode) {
		super(mode);
	}

	public BusImplWrapper(String configLocation, BusMode mode) {
		super(configLocation, mode);
	}
}
