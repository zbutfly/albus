package net.butfly.bus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Constants.Side;
import net.butfly.bus.policy.Routeable;

public final class ServerWrapper implements Routeable {
	private static final Set<ServerWrapper> ALL = new HashSet<ServerWrapper>();
	private final Map<String, Bus> servers = new HashMap<String, Bus>();

	@SuppressWarnings("unchecked")
	public static ServerWrapper construct(String configLocations, String serverClassName) {
		ServerWrapper wrapper = new ServerWrapper();
		Class<? extends Bus> serverClass;
		try {
			serverClass = (Class<? extends Bus>) Class.forName(serverClassName);
		} catch (Exception e) {
			serverClass = Bus.class;
		}
		if (configLocations == null) wrapper.registerSingle(null, serverClass);
		else for (String conf : configLocations.split(","))
			if (!"".equals(conf.trim())) wrapper.registerSingle(conf, serverClass);
		ALL.add(wrapper);
		return wrapper;
	}

	public Bus server(String serverId) {
		return servers.get(serverId);
	}

	public Bus[] servers() {
		return servers.values().toArray(new Bus[servers.values().size()]);
	}

	@Override
	public String id() {
		return null;
	}

	@Override
	public String[] supportedTXs() {
		Set<String> all = new HashSet<String>();
		for (Bus server : servers.values())
			all.addAll(Arrays.asList(server.supportedTXs()));
		return all.toArray(new String[all.size()]);
	}

	private void registerSingle(String conf, Class<? extends Bus> serverClass) {
		Bus server;
		try {
			server = serverClass.getConstructor(String.class, Side.class).newInstance(conf, Side.SERVER);;
		} catch (Exception e) {
			throw new SystemException("", "Could not construct bus server instance.", e);
		}
		servers.put(server.id(), server);
	}

	private ServerWrapper() {}
}
