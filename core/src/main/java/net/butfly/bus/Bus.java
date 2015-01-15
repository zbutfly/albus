package net.butfly.bus;

import net.butfly.albacore.utils.ReflectionUtils.MethodInfo;
import net.butfly.bus.policy.Routeable;

public interface Bus extends Routeable {
	public enum Mode {
		SERVER, CLIENT;
	}

	MethodInfo invokeInfo(String code, String version);
}
