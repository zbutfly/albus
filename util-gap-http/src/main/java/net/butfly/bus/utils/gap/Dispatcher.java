package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.undertow.Undertow;
import net.butfly.albacore.utils.parallel.Concurrents;

/**
 * 这是外网口
 * 
 * @author butfly
 */
public class Dispatcher extends GapTunnelOstium {
	private final Undertow server;
	private final Map<UUID, Response> sessions;

	public static void main(String[] args) throws IOException {
		Dispatcher inst = new Dispatcher(args.length < 1 ? "bus-gap-dispatcher.properties" : args[0]);
		inst.watcher.run();
		inst.server.start();
	}

	protected Dispatcher(String conf) throws IOException {
		super(conf, "bus.gap.dispatcher.", ".resp", ".req");
		sessions = new ConcurrentHashMap<>();
		logger().info("GAP-Dispatcher start on [" + host + ":" + port + "]");
		server = Undertow.builder().addHttpListener(port, host).setHandler(exch -> {
			UUID key = UUID.randomUUID();
			logger().trace(exch.toString());
			tunnel.writing(key.toString() + dumpExt, new Request(exch)::writeTo);
			Response resp;
			while ((resp = sessions.remove(key)) == null)
				Concurrents.waitSleep();
			resp.response(exch);
		}).build();
	}

	@Override
	public void seen(UUID key, InputStream data) {
		sessions.put(key, Response.readFrom(data));
		logger().debug("Pool size: " + sessions.size());
	}
}
