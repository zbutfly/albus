package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import net.butfly.bus.utils.http.HttpClient;
import net.butfly.bus.utils.http.Request;
import net.butfly.bus.utils.http.Response;

/**
 * 这是内网口
 * 
 * @author butfly
 */
public class Invoker extends WaiterImpl {
	private final HttpClient client;

	public static void main(String[] args) throws IOException, InterruptedException {
		Invoker inst = new Invoker();
		new Thread(inst).join();
	}

	protected Invoker() throws IOException {
		super("bus.gap.invoker.", ".req", ".resp");
		client = new HttpClient();
	}

	@Override
	public void seen(UUID key, InputStream data) {
		Request.readFrom(data).redirect(host, port).request(client, resp -> {
			try {
				touch(dumpDest, key.toString() + touchExt, new Response(resp)::writeTo);
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
