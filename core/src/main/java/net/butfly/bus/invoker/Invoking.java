package net.butfly.bus.invoker;

import java.util.Map;

import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Bus;
import net.butfly.bus.TX;

public final class Invoking {
	public TX tx;
	public boolean supportClass;
	public Options options;
	public Map<String, String> context;
	public Bus bus;
	public Class<?>[] parameterClasses;
	public Object[] parameters;
}
