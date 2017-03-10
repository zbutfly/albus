package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 这是内网口
 * 
 * @author butfly
 */
public class Invoker extends GapTunnelOstium {
	private final HttpClient client;

	public static void main(String[] args) throws IOException, InterruptedException {
		Invoker inst = new Invoker(args.length < 1 ? "bus-gap-dispatcher.properties" : args[0]);
		inst.watcher.start();
		inst.watcher.join();
	}

	protected Invoker(String conf) throws IOException {
		super(conf, "bus.gap.invoker.", ".req", ".resp");
		client = new HttpClient();
	}

	@Override
	public void seen(UUID key, InputStream data) {
		Request.readFrom(data).redirect(host, port).request(client, resp -> {
			try {
				toucher.touch(key.toString() + touchExt, new Response(resp)::writeTo);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
