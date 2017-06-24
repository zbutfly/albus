package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import net.butfly.albacore.io.Udps;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.IOs;
import net.butfly.albacore.utils.parallel.Concurrents;

/**
 * 这是外网口
 * 
 * @author butfly
 */
@Config(value = "bus-gap-dispatcher.properties", prefix = "bus.gap.dispatcher")
public class Dispatcher extends WaiterImpl {
	private final DatagramChannel server;
	private final Selector selector;
	private final Map<UUID, Consumer<InputStream>> handlers;

	public static void main(String[] args) throws IOException, InterruptedException {
		Dispatcher inst = new Dispatcher(args);
		inst.start();
		inst.join();
	}

	protected Dispatcher(String... args) throws IOException {
		super(EXT_RESP, EXT_REQ, args);
		handlers = new ConcurrentHashMap<>();
		server = DatagramChannel.open();
		server.bind(Udps.UDP_DEFAULT_SERV);
		selector = Selector.open();
		server.register(selector, SelectionKey.OP_READ);
	}

	private void handle(DatagramChannel ch) throws IOException {
		UUID key = UUID.randomUUID();
		ByteBuffer rbuf = ByteBuffer.allocate(Udps.UDP_DIAGRAM_MAX_LEN);
		InetSocketAddress addr = (InetSocketAddress) ch.receive(rbuf);
		AtomicBoolean finished = new AtomicBoolean(false);
		if (null != handlers.putIfAbsent(key, in -> send(ch, addr, in))) //
			logger().error("Resp/Req key [" + key + "] duplicated, current request lost...");
		else {
			logger().debug("Request [" + key + "] arrive, pool size: " + handlers.size());
			touch(key.toString() + touchExt, out -> write(out, rbuf, finished));
			while (!finished.get())
				Concurrents.waitSleep(10);
			logger().debug("Response [" + key + "] sent.");
		}

	}

	private void send(DatagramChannel ch, InetSocketAddress addr, InputStream in) {
		try {
			ch.send(ByteBuffer.wrap(IOs.readAll(in)), addr);
		} catch (IOException e) {}
	}

	private void write(OutputStream out, ByteBuffer buf, AtomicBoolean flag) {
		try {
			out.write(buf.array());
		} catch (IOException e) {} finally {
			flag.set(true);
		}
	}

	@Override
	public void seen(UUID key, InputStream in) {
		Consumer<InputStream> h = handlers.remove(key);
		if (null != h) {
			logger().debug("Response [" + key + "] left, pool size: " + handlers.size());
			h.accept(in);
		} else logger().error("Resp/Req key [" + key + "] not found, current response lost...");
	}

	@Override
	public void run() {
		while (true) {
			try {
				int n = selector.select();
				if (n > 0) {
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey k = it.next();
						it.remove();
						if (k.isReadable()) handle((DatagramChannel) k.channel());
					}
				}
			} catch (Exception e) {}
		}
	}
}
