package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import net.butfly.albacore.utils.Configs.Config;
import net.butfly.bus.utils.http.HttpClient;
import net.butfly.bus.utils.http.HttpRequest;
import net.butfly.bus.utils.http.HttpResponse;

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
		super(".req", ".resp", args);
		client = new HttpClient();
	}

	@Override
	public void seen(UUID key, InputStream in) {
		new HttpRequest().load(in).redirect(host, port).request(client, resp -> {
			try {
				touch(dumpDest, key.toString() + touchExt, new HttpResponse(resp)::save);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void run() {
		try {
			watcher.join();
		} catch (InterruptedException e) {}
	}
}
