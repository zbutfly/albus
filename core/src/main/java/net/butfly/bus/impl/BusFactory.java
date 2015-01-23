package net.butfly.bus.impl;

import net.butfly.bus.Bus;

public final class BusFactory {
	private BusFactory() {}

	enum Mode {
		SERVER, CLIENT;
	}

	public static Bus client() {
		return create(null, Mode.CLIENT);
	}

	public static Bus client(String conf) {
		return create(conf, Mode.CLIENT);
	}

	public static Bus server() {
		return create(null, Mode.SERVER);
	}

	public static Bus server(String conf) {
		return create(conf, Mode.SERVER);
	}

	static Bus create(String conf, Mode mode) {
		return new BusImpl(conf, mode);
	}
}
