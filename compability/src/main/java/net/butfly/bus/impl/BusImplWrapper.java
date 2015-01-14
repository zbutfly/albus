package net.butfly.bus.impl;

import net.butfly.bus.Bus;

public class BusImplWrapper extends BusImpl implements Bus {
	private static final long serialVersionUID = -8836827099838805984L;

	public BusImplWrapper(BusMode mode) {
		super(mode);
	}

	public BusImplWrapper(String configLocation, BusMode mode) {
		super(configLocation, mode);
	}
}
