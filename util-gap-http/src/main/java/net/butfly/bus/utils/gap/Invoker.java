package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import net.butfly.albacore.utils.Configs.Config;
import net.butfly.bus.utils.http.HttpClient;
import net.butfly.bus.utils.http.HttpRequest;
import net.butfly.bus.utils.http.HttpWaiter;

/**
 * 这是内网口
 * 
 * @author butfly
 */
@Config(value = "bus-gap-invoker.properties", prefix = "bus.gap.invoker")
public class Invoker extends HttpWaiter {
	private final HttpClient client;

	public static void main(String[] args) throws IOException, InterruptedException {
		Invoker inst = new Invoker(HttpWaiter.parseArgs(args));
		inst.start();
		inst.join();
	}

	protected Invoker(HttpWaiter.HttpWaiterConfig config) throws IOException {
		super(EXT_REQ, EXT_RESP, config);
		logger().info("Starting on [" + addr.toString() + "], allow methods: [" + methods.toString() + "] data dump to [" + dumpDest + "]");
		if (config.watchs.length > 1) logger().error("Multiple path to be watching defined but only support first [" + config.watchs[0]
				+ "] now....");
		client = new HttpClient();
	}

	@Override
	public void seen(UUID key, InputStream in) {
		HttpRequest r = new HttpRequest().load(in);
		if (methods.contains(r.method().toUpperCase())) r.redirect(addr.getHostName(), addr.getPort()).request(client, resp -> {
			try {
				touch(key.toString() + touchExt, resp::save);
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
