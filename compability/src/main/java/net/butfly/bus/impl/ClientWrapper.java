package net.butfly.bus.impl;

import net.butfly.bus.StandardBus;
import net.butfly.bus.impl.BusFactory.Mode;

public class ClientWrapper extends StandardBusImpl implements StandardBus {
	private static final long serialVersionUID = -8836827099838805984L;

	public ClientWrapper() {
		super(Mode.CLIENT);
	}

	public ClientWrapper(String configLocation) {
		super(configLocation, Mode.CLIENT);
	}
}
