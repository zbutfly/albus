package net.butfly.bus.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SimpleContext extends Context {
	private Map<String, Object> impl = new ConcurrentHashMap<String, Object>();

	@Override
	protected Context current() {
		return this;
	};

	@Override
	protected Map<String, Object> impl() {
		return impl;
	}

	@Override
	protected boolean sharing() {
		return false;
	}
}
