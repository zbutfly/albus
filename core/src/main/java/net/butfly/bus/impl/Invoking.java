package net.butfly.bus.impl;

import java.util.Map;

import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Bus;
import net.butfly.bus.TX;

final class Invoking {
	public TX tx;
	public boolean supportClass;
	public Options options;
	public Map<String, String> context;
	public Bus bus;
	public Class<?>[] parameterClasses;
	public Object[] parameters;
}
