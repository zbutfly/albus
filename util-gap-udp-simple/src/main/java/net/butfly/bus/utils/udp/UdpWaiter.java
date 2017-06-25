package net.butfly.bus.utils.udp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.IOs;
import net.butfly.albacore.utils.Systems;
import net.butfly.albacore.utils.parallel.Concurrents;
import net.butfly.bus.utils.gap.WaiterImpl;

public abstract class UdpWaiter extends WaiterImpl {
	protected final int gapPort;
	// protected final int kcpPort;
	protected final DatagramChannel server;
	protected final Selector servers;
	// protected final DatagramChannel client;
	// protected final Selector clients;
	public static final int UDP_DIAGRAM_MAX_LEN = 0xFFFF - 8 - 20;
	private final Map<String, Consumer<InputStream>> handlers;

	protected UdpWaiter(String watchExt, String touchExt, int listenPort, Path dumpDest, Path... watchs) throws IOException {
		super(watchExt, touchExt, dumpDest, watchs);
		this.gapPort = listenPort;
		// this.kcpPort = kcpPort;
		handlers = new ConcurrentHashMap<>();

		server = DatagramChannel.open();
		server.configureBlocking(false);
		server.bind(new InetSocketAddress("127.0.0.1", gapPort));
		servers = Selector.open();
		server.register(servers, SelectionKey.OP_READ);
	}

	private DatagramChannel client(int remotePort) throws IOException {
		DatagramChannel client = DatagramChannel.open();
		client.configureBlocking(false);
		client.connect(new InetSocketAddress("127.0.0.1", remotePort));
		Selector clients = Selector.open();
		client.register(clients, SelectionKey.OP_READ);
		return client;
	}

	protected static List<Path> parse(String... args) {
		List<String> paths = new ArrayList<>(Arrays.asList(args));
		if (paths.size() >= 1) paths.remove(0);
		if (paths.size() == 0) paths.add(Configs.get("dst"));
		if (paths.size() == 1) paths.add(Configs.get("src"));
		return paths.stream().map(s -> Paths.get(s)).collect(Collectors.toList());
	}

	protected static void help(String... args) {
		System.err.println("Usage: ");
		System.err.println("\tjava " + Systems.getMainClass());
		System.err.println("\tjava " + Systems.getMainClass() + " <GAP_PORT>");
		System.err.println("\tjava " + Systems.getMainClass() + " <GAP_PORT> <KCP_PORT>");
		System.err.println("\tjava " + Systems.getMainClass() + " <GAP_PORT> <KCP_PORT> <DUMP_PATH> <WATCH_PATH> <WATCH_PATH>...");
	}

	protected void write(OutputStream out, ByteBuffer buf, AtomicBoolean flag) {
		try {
			out.write(buf.array());
		} catch (IOException e) {} finally {
			flag.set(true);
		}
	}

	protected void send(DatagramChannel ch, InetSocketAddress addr, InputStream in) {
		try {
			ch.send(ByteBuffer.wrap(IOs.readAll(in)), addr);
		} catch (IOException e) {}
	}

	@Override
	protected void seen(String key, InputStream in) throws IOException {
		int remotePort = Integer.parseInt(key.split("#", 2)[0]);
		int i = client(remotePort).write(ByteBuffer.wrap(IOs.readAll(in)));
		logger().debug("Send [" + i + "] bytes to: " + remotePort);
	}

	@Override
	public void run() {
		while (true) {
			try {
				int n = servers.select();
				if (n > 0) {
					Iterator<SelectionKey> it = servers.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey k = it.next();
						it.remove();
						if (k.isReadable()) {
							DatagramChannel ch = (DatagramChannel) k.channel();
							ByteBuffer rbuf = ByteBuffer.allocate(UdpWaiter.UDP_DIAGRAM_MAX_LEN);
							InetSocketAddress addr = (InetSocketAddress) ch.receive(rbuf);

							String key = addr.getPort() + "#" + UUID.randomUUID().toString();
							logger().debug("Recv from " + addr.toString());
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
					}
				}
			} catch (Exception e) {
				logger().error("Recv failed on save.", e);
			}
		}
	}
}
