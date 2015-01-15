package net.butfly.bus;

import net.butfly.albacore.utils.ReflectionUtils.MethodInfo;
import net.butfly.bus.policy.Routeable;

public interface Bus extends Routeable {
	MethodInfo invokeInfo(String code, String version);
}
