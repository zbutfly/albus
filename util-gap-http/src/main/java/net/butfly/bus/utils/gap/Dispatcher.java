package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.parallel.Concurrents;
import net.butfly.bus.utils.http.HttpRequest;
import net.butfly.bus.utils.http.HttpResponse;
import net.butfly.bus.utils.http.HttpWaiter;

/**
 * 这是外网口
 * 
 * @author butfly
 */
@Config(value = "bus-gap-dispatcher.properties", prefix = "bus.gap.dispatcher")
public class Dispatcher extends HttpWaiter {
	private final Undertow server;
	private final Map<UUID, Consumer<HttpResponse>> handlers;

	public static void main(String[] args) throws IOException, InterruptedException {
		Dispatcher inst = new Dispatcher(HttpWaiter.parseArgs(args));
		inst.start();
		inst.join();
	}

	protected Dispatcher(HttpWaiter.HttpWaiterConfig config) throws IOException {
		super(EXT_RESP, EXT_REQ, config);
		handlers = new ConcurrentHashMap<>();
		server = Undertow.builder().addHttpListener(addr.getPort(), addr.getHostName()).setHandler(exch -> this.handle(exch)).build();
	}

	private void handle(HttpServerExchange exch) throws IOException {
		if (methods.contains(exch.getRequestMethod().toString().toUpperCase())) {
			UUID key = UUID.randomUUID();
			logger().trace(exch.toString());
			AtomicBoolean finished = new AtomicBoolean(false);
			if (null != handlers.putIfAbsent(key, resp -> finished.set(resp.response(exch)))) {
				logger().error("Resp/Req key [" + key + "] duplicated, current request lost...");
				exch.setStatusCode(500);
				exch.setReasonPhrase("HTTP request through GAP duplicate.");
			} else {
				logger().debug("Request [" + key + "] arrive, pool size: " + handlers.size());
				touch(key.toString() + touchExt, new HttpRequest(exch)::save);
				while (!finished.get())
					Concurrents.waitSleep(10);
				logger().debug("Response [" + key + "] sent.");
			}
		} else logger().warn("HTTP request forbidden for method: " + exch.getRequestMethod());
	}

	@Override
	public void seen(String key, InputStream data) {
		Consumer<HttpResponse> h = handlers.remove(UUID.fromString(key));
		if (null != h) {
			logger().debug("Response [" + key + "] left, pool size: " + handlers.size());
			h.accept(new HttpResponse().load(data));
		} else logger().error("Resp/Req key [" + key + "] not found, current response lost...");
	}

	@Override
	public void run() {
		server.start();
	}
}
