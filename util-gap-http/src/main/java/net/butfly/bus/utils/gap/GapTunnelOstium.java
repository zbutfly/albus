package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;

import net.butfly.albacore.utils.Configs;

/**
 * 这是网闸上的一个通道（口）
 * 
 * @author butfly
 */
public abstract class GapTunnelOstium implements TunnelOstium {
	private final String confPrefix;
	protected final String host;
	protected final int port;
	protected final GapTunnel tunnel;
	protected final Watcher watcher;
	protected final String dumpExt;

	protected GapTunnelOstium(String conf, String confPrefix, String watchExt, String dumpExt) throws IOException {
		this.confPrefix = confPrefix;
		Configs.setConfig(conf);
		host = conf("host", "0.0.0.0");
		port = Integer.parseInt(conf("port", "80"));
		this.dumpExt = dumpExt;
		tunnel = new GapTunnel(Paths.get(conf("src", "./pool")));
		watcher = new Watcher(this::reading, Paths.get(conf("src", "./pool")), watchExt, StandardWatchEventKinds.ENTRY_CREATE);
		logger().info("Starting on [" + host + ":" + port + "]");
	}

	private String conf(String key, String def) {
		return Configs.MAIN_CONF.getOrDefault(confPrefix + key, def);
	}
}
