package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.UUID;

import net.butfly.albacore.io.Udps;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.IOs;

/**
 * 这是内网口
 * 
 * @author butfly
 */
@Config(value = "bus-gap-invoker.properties", prefix = "bus.gap.invoker")
public class Invoker extends WaiterImpl {
	private final DatagramChannel client;
	private final Selector selector;

	public static void main(String[] args) throws IOException, InterruptedException {
		Invoker inst = new Invoker(args);
		inst.start();
		inst.join();
	}

	protected Invoker(String... args) throws IOException {
		super(EXT_REQ, EXT_RESP, args);
		client = DatagramChannel.open();
		client.configureBlocking(false);
		client.connect(Udps.UDP_DEFAULT_SERV);
		selector = Selector.open();
		client.register(selector, SelectionKey.OP_READ);
		client.write(Charset.defaultCharset().encode("data come from client"));
	}

	@Override
	public void seen(UUID key, InputStream in) {
		try {
			int n = selector.select();
			if (n > 0) {
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey k = it.next();
					it.remove();
					if (k.isReadable()) {
						DatagramChannel ch = (DatagramChannel) k.channel();
						ch.write(ByteBuffer.wrap(IOs.readAll(in)));
						ByteBuffer buf = ByteBuffer.allocate(Udps.UDP_DIAGRAM_MAX_LEN);
						ch.read(buf);
						touch(key.toString() + touchExt, out -> {
							try {
								out.write(buf.array());
							} catch (IOException e) {}
						});
					}
				}
			}
		} catch (Exception e) {}
	}

	@Override
	public void run() {
		watcher.joining();
	}
}
