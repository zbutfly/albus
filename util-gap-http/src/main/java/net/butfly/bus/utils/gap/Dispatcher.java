package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.parallel.Concurrents;
import net.butfly.bus.utils.http.HttpRequest;
import net.butfly.bus.utils.http.HttpResponse;

/**
 * 这是外网口
 * 
 * @author butfly
 */
@Config(value = "bus-gap-dispatcher.properties", prefix = "bus.gap.dispatcher")
public class Dispatcher extends WaiterImpl {
	private final Undertow server;
	private final Map<UUID, HttpResponse> resps;
	private final AtomicLong count;

	public static void main(String[] args) throws IOException, InterruptedException {
		Dispatcher inst = new Dispatcher(args);
		inst.start();
		inst.join();
	}

	protected Dispatcher(String... args) throws IOException {
		super(".resp", ".req", args);
		count = new AtomicLong();
		resps = new ConcurrentHashMap<>();
		server = Undertow.builder().addHttpListener(port, host).setHandler(exch -> this.handle(exch)).build();
	}

	private void handle(HttpServerExchange exch) throws IOException {
		UUID key;
		logger().debug("Req [" + (key = UUID.randomUUID()) + "] arrive, pending requests: " + count.incrementAndGet());
		try {
			logger().trace(exch.toString());
			touch(dumpDest, key.toString() + touchExt, new HttpRequest(exch)::save);
			HttpResponse resp;
			while ((resp = resps.remove(key)) == null)
				Concurrents.waitSleep();
			resp.response(exch);
		} finally {
			logger().debug("Req [" + key + "] left, pending requests: " + count.decrementAndGet());
		}
	}

	@Override
	public void seen(UUID key, InputStream data) {
		resps.put(key, new HttpResponse().load(data));
		logger().debug("Pool size: " + resps.size());
	}

	@Override
	public void run() {
		server.start();
	}
}
