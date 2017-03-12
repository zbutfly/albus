package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.undertow.Undertow;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.parallel.Concurrents;
import net.butfly.bus.utils.http.Request;
import net.butfly.bus.utils.http.Response;

/**
 * 这是外网口
 * 
 * @author butfly
 */
@Config(value = "bus-gap-dispatcher.properties", prefix = "bus.gap.dispatcher")
public class Dispatcher extends WaiterImpl {
	private final Undertow server;
	private final Map<UUID, Response> sessions;

	public static void main(String[] args) throws IOException, InterruptedException {
		Dispatcher inst = new Dispatcher();
		inst.start();
		inst.join();
	}

	protected Dispatcher() throws IOException {
		super(".resp", ".req");
		sessions = new ConcurrentHashMap<>();
		server = Undertow.builder().addHttpListener(port, host).setHandler(exch -> {
			UUID key = UUID.randomUUID();
			logger().trace(exch.toString());
			touch(dumpDest, key.toString() + touchExt, new Request(exch)::writeTo);
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

	@Override
	public void run() {
		server.start();
	}
}
