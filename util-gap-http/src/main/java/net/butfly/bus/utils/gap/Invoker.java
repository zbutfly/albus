package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import net.butfly.albacore.utils.Configs.Config;
import net.butfly.bus.utils.http.HttpClient;
import net.butfly.bus.utils.http.HttpRequest;

/**
 * 这是内网口
 * 
 * @author butfly
 */
@Config(value = "bus-gap-invoker.properties", prefix = "bus.gap.invoker")
public class Invoker extends WaiterImpl {
	private final HttpClient client;

	public static void main(String[] args) throws IOException, InterruptedException {
		Invoker inst = new Invoker(args);
		inst.start();
		inst.join();
	}

	protected Invoker(String... args) throws IOException {
		super(EXT_REQ, EXT_RESP, args);
		client = new HttpClient();
	}

	@Override
	public void seen(UUID key, InputStream in) {
		HttpRequest r = new HttpRequest().load(in);
		if (methods.contains(r.method().toUpperCase())) r.redirect(host, port).request(client, resp -> {
			try {
				touch(dumpDest, key.toString() + touchExt, resp::save);
			} catch (IOException e) {
				logger().error("HTTP request fail", e);
			}
		});
		else logger().warn("HTTP request [" + key + "] forbidden for method: " + r.method());
	}

	@Override
	public void run() {
		watcher.joining();
	}
}
