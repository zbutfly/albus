package net.butfly.bus.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.BasicBus;
import net.butfly.bus.policy.Routeable;

public final class ServerWrapper implements Routeable {
	private static final Set<ServerWrapper> ALL = new HashSet<ServerWrapper>();
	private final Map<String, BasicBus> servers = new HashMap<String, BasicBus>();

	@SuppressWarnings("unchecked")
	public static ServerWrapper construct(String configLocations, String serverClassName) {
		ServerWrapper wrapper = new ServerWrapper();
		Class<? extends BasicBus> serverClass;
		try {
			serverClass = (Class<? extends BasicBus>) Class.forName(serverClassName);
		} catch (Exception e) {
			serverClass = BasicBus.class;
		}
		if (configLocations == null) wrapper.registerSingle(null, serverClass);
		else for (String conf : configLocations.split(","))
			if (!"".equals(conf.trim())) wrapper.registerSingle(conf, serverClass);
		ALL.add(wrapper);
		return wrapper;
	}

	public BasicBus server(String serverId) {
		return servers.get(serverId);
	}

	public BasicBus[] servers() {
		return servers.values().toArray(new BasicBus[servers.values().size()]);
	}

	@Override
	public String id() {
		return null;
	}

	@Override
	public String[] supportedTXs() {
		Set<String> all = new HashSet<String>();
		for (BasicBus server : servers.values())
			all.addAll(Arrays.asList(server.supportedTXs()));
		return all.toArray(new String[all.size()]);
	}

	private void registerSingle(String conf, Class<? extends BasicBus> serverClass) {
		BasicBus server;
		try {
			server = serverClass.getConstructor(String.class).newInstance(conf);
		} catch (Exception e) {
			throw new SystemException("", "Could not construct bus server instance.", e);
		}
		servers.put(server.id(), server);
	}

	private ServerWrapper() {}
}
