package net.butfly.bus.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.bus.policy.Routeable;

public final class Cluster implements Routeable {
	private final Map<String, BusImpl> nodes = new HashMap<String, BusImpl>();

	public Cluster(String configLocations) {
		if (configLocations == null) this.registerSingle(null);
		else for (String conf : configLocations.split(","))
			if (!"".equals(conf.trim())) this.registerSingle(conf);
	}

	public BusImpl[] servers() {
		return nodes.values().toArray(new BusImpl[nodes.values().size()]);
	}

	@Override
	public String id() {
		return null;
	}

	@Override
	public String[] supportedTXs() {
		Set<String> all = new HashSet<String>();
		for (BusImpl impl : nodes.values())
			all.addAll(Arrays.asList(impl.supportedTXs()));
		return all.toArray(new String[all.size()]);
	}

	private void registerSingle(String conf) {
		BusImpl impl = (BusImpl) BusFactory.bus(conf);
		nodes.put(impl.id(), impl);
	}
}
