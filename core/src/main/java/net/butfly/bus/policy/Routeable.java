package net.butfly.bus.policy;

public interface Routeable {
	String id();

	String[] supportedTXs();
}
