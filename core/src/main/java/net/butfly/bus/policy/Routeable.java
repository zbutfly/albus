package net.butfly.bus.policy;

public interface Routeable {
	String id();

	boolean isSupported(String tx);
}
