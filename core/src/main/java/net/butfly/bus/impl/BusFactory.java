package net.butfly.bus.impl;

import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.bus.Bus;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.StandardBus;

public final class BusFactory {
	private BusFactory() {}

	public static <T extends Bus> T bus(Class<T> type) {
		return bus(type, BusMode.CLIENT);
	}

	public static <T extends Bus> T bus(Class<T> type, String conf) {
		return bus(type, conf, BusMode.CLIENT);
	}

	public static <T extends Bus> T bus(Class<T> type, BusMode mode) {
		return bus(type, null, mode);
	}

	public static <T extends Bus> T bus(Class<T> type, String conf, BusMode mode) {
		return ReflectionUtils.safeConstruct(type, ReflectionUtils.parameters(String.class, conf),
				ReflectionUtils.parameters(BusMode.class, mode));
	}

	public static StandardBus standard() {
		return standard(BusMode.CLIENT);
	}

	public static StandardBus standard(String conf) {
		return standard(conf, BusMode.CLIENT);
	}

	public static StandardBus standard(BusMode mode) {
		return standard(null, mode);
	}

	public static StandardBus standard(String conf, BusMode mode) {
		return new StandardBusImpl(conf, mode);
	}

	public static CallbackBus callback() {
		return callback(BusMode.CLIENT);
	}

	public static CallbackBus callback(String conf) {
		return callback(conf, BusMode.CLIENT);
	}

	public static CallbackBus callback(BusMode mode) {
		return callback(null, mode);
	}

	public static CallbackBus callback(String conf, BusMode mode) {
		return new CallbackBusImpl(conf, mode);
	}
}
