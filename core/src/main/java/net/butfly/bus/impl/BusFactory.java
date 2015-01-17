package net.butfly.bus.impl;

import net.butfly.bus.CallbackBus;

public final class BusFactory {
	private BusFactory() {}

	enum Mode {
		SERVER, CLIENT;
	}

	public static CallbackBus client() {
		return create(null, Mode.CLIENT);
	}

	public static CallbackBus client(String conf) {
		return create(conf, Mode.CLIENT);
	}

	public static CallbackBus server() {
		return create(null, Mode.SERVER);
	}

	public static CallbackBus server(String conf) {
		return create(conf, Mode.SERVER);
	}

	static CallbackBus create(String conf, Mode mode) {
		return new CallbackBusImpl(conf, mode);
	}
}
