package net.butfly.bus.utils.gap;

import static net.butfly.bus.utils.gap.KcpWaiter.UDP_DIAGRAM_MAX_LEN;
import static net.butfly.bus.utils.gap.KcpWaiter.help;
import static net.butfly.bus.utils.gap.KcpWaiter.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.IOs;
import net.butfly.albacore.utils.parallel.Concurrents;

@Config(value = "bus-gap-invoker.properties", prefix = "bus.gap.invoker")
public class Invoker extends WaiterImpl {
	private int gapPort;
	private final DatagramSocket client;

	public static void main(String[] args) throws IOException, InterruptedException {
		help();
		int kcpServerPort = Integer.parseInt(args.length > 0 ? args[0] : Configs.get("port"));
		List<Path> paths = parse(args);
		Invoker inst = new Invoker(kcpServerPort, paths.remove(0), paths.toArray(new Path[paths.size()]));
		inst.start();
		inst.join();
	}

	protected Invoker(int gapPort, Path dest, Path... srcs) throws IOException {
		super(EXT_REQ, EXT_RESP, dest, srcs);
		this.gapPort = gapPort;
		logger().info("Start gap@kcp invoker, listen (watch): [" + srcs[0] + "], dump: [" + dest.toString()
				+ "]\n\tListen for diagrams from Invoker:kcp-server|Dispatcher:kcp-client on port [" + this.gapPort + "].");
		client = new DatagramSocket();
		client.connect(new InetSocketAddress("127.0.0.1", gapPort));
	}

	@Override
	protected void seen(String key, InputStream in) throws IOException {
		byte[] data = IOs.readAll(in);
		DatagramPacket packet = new DatagramPacket(data, data.length);
		client.send(packet);
	}

	@Override
	public void run() {
		byte[] buf = new byte[UDP_DIAGRAM_MAX_LEN];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while (true) {
			try {
				client.receive(packet);
				logger().debug("client receive packet:" + packet.getLength() + " from " + packet.getAddress() + ":" + packet.getPort());
				String key = UUID.randomUUID().toString();
				AtomicBoolean finished = new AtomicBoolean(false);
				touch(key + touchExt, out -> write(out, packet.getData(), packet.getLength(), finished));
				while (!finished.get())
					Concurrents.waitSleep(10);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void write(OutputStream out, byte[] data, int len, AtomicBoolean flag) {
		try {
			out.write(data, 0, len);
		} catch (IOException ignored) {} finally {
			flag.set(true);
		}
	}
}
