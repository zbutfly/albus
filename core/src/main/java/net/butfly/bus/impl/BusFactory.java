package net.butfly.bus.impl;

import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.bus.Bus;
import net.butfly.bus.Bus.Mode;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.StandardBus;

public final class BusFactory {
	private BusFactory() {}

	public static <T extends Bus> T bus(Class<T> type) {
		return bus(type, Mode.CLIENT);
	}

	public static <T extends Bus> T bus(Class<T> type, String conf) {
		return bus(type, conf, Mode.CLIENT);
	}

	public static <T extends Bus> T bus(Class<T> type, Mode mode) {
		return bus(type, null, mode);
	}

	public static <T extends Bus> T bus(Class<T> type, String conf, Mode mode) {
		return ReflectionUtils.safeConstruct(type, ReflectionUtils.parameters(String.class, conf),
				ReflectionUtils.parameters(Mode.class, mode));
	}

	public static StandardBus standard() {
		return standard(Mode.CLIENT);
	}

	public static StandardBus standard(String conf) {
		return standard(conf, Mode.CLIENT);
	}

	public static StandardBus standard(Mode mode) {
		return standard(null, mode);
	}

	public static StandardBus standard(String conf, Mode mode) {
		return new StandardBusImpl(conf, mode);
	}

	public static CallbackBus callback() {
		return callback(Mode.CLIENT);
	}

	public static CallbackBus callback(String conf) {
		return callback(conf, Mode.CLIENT);
	}

	public static CallbackBus callback(Mode mode) {
		return callback(null, mode);
	}

	public static CallbackBus callback(String conf, Mode mode) {
		return new CallbackBusImpl(conf, mode);
	}
}
