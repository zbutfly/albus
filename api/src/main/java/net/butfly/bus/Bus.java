package net.butfly.bus;

public interface Bus extends StandardBus, CallbackBus {
	public enum Mode {
		SERVER, CLIENT;
	}
}
