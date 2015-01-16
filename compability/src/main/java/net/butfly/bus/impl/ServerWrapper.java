package net.butfly.bus.impl;

import net.butfly.bus.StandardBus;
import net.butfly.bus.impl.BusFactory.Mode;

public class ServerWrapper extends StandardBusImpl implements StandardBus {
	private static final long serialVersionUID = -8836827099838805984L;

	public ServerWrapper() {
		super(Mode.SERVER);
	}

	public ServerWrapper(String configLocation) {
		super(configLocation, Mode.SERVER);
	}
}
