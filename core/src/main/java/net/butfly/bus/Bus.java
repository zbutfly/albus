package net.butfly.bus;

import net.butfly.bus.policy.Routeable;

public interface Bus extends Routeable {
	public enum Mode {
		SERVER, CLIENT;
	}
}
