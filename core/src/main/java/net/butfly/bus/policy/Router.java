package net.butfly.bus.policy;

public interface Router {
	@SuppressWarnings("unchecked")
	<T extends Routeable> T route(String requestTX, T... possiable);
}
