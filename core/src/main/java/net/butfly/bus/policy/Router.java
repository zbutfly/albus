package net.butfly.bus.policy;

public interface Router {
	<T extends Routeable> T route(String requestTX, T[] possiable);
}
