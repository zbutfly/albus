package net.butfly.bus.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.bus.Bus;
import net.butfly.bus.policy.Routeable;

public final class Cluster implements Routeable {
	private final Map<String, Bus> nodes = new HashMap<String, Bus>();
	private BusMode mode;

	public Cluster(boolean supportCallback, String... config) {
		this(BusMode.CLIENT, supportCallback, config);
	}

	public Cluster(BusMode mode, boolean supportCallback, String... config) {
		this.mode = mode;
		Class<? extends Bus> busClass = supportCallback ? CallbackBusImpl.class : StandardBusImpl.class;
		if (config == null || config.length == 0) this.registerSingle(null, busClass);
		else for (String conf : config)
			if (!"".equals(conf.trim())) this.registerSingle(conf, busClass);
	}

	public Bus[] servers() {
		return nodes.values().toArray(new Bus[nodes.values().size()]);
	}

	@Override
	public String id() {
		return null;
	}

	@Override
	public String[] supportedTXs() {
		Set<String> all = new HashSet<String>();
		for (Bus impl : nodes.values())
			all.addAll(Arrays.asList(impl.supportedTXs()));
		return all.toArray(new String[all.size()]);
	}

	private void registerSingle(String conf, Class<? extends Bus> busClass) {
		Bus impl = (Bus) BusFactory.bus(busClass, conf, mode);
		nodes.put(impl.id(), impl);
	}
}
