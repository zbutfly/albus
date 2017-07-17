package net.butfly.bus.utils.gap;

import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.IOs;
import net.butfly.albacore.utils.parallel.Concurrents;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.butfly.bus.utils.gap.KcpWaiter.parse;

@Config(value = "bus-gap-dispatcher.properties", prefix = "bus.gap.dispatcher")
public class Dispatcher extends WaiterImpl {

	private final int gapPort;
	private final DatagramSocket server;
	public static final int UDP_DIAGRAM_MAX_LEN = 0xFFFF - 8 - 20;
	private InetAddress remoteAddr;
	private int remotePort;

	public static void main(String[] args) throws IOException, InterruptedException {
		KcpWaiter.help(args);
		int dispatcherPort = Integer.parseInt(args.length > 0 ? args[0] : Configs.get("port"));
		List<Path> paths = parse(args);
		Dispatcher inst = new Dispatcher(dispatcherPort, paths.remove(0), paths.toArray(new Path[paths.size()]));
		inst.start();
		inst.join();
	}

	protected Dispatcher(int gapPort, Path dumpDest, Path... watchs) throws IOException {
		super(EXT_RESP, EXT_REQ, dumpDest, watchs);
		this.gapPort = gapPort;
		server = new DatagramSocket(this.gapPort, InetAddress.getByName("127.0.0.1"));
	}

	@Override
	public void run() {
		byte[] buf = new byte[UDP_DIAGRAM_MAX_LEN];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while (true) {
			try {
				server.receive(packet);
				remoteAddr = packet.getAddress();
				remotePort = packet.getPort();
				logger().debug("server receive packet:" + packet.getLength() + " from " + packet.getAddress() + ":" + packet.getPort());
				String key = UUID.randomUUID().toString();
				byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
				AtomicBoolean finished = new AtomicBoolean(false);
				touch(key + touchExt, out -> write(out, data, finished));
				while (!finished.get()) {
					Concurrents.waitSleep(10);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void seen(String key, InputStream in) throws IOException {
		byte[] buf = IOs.readAll(in);
		logger().debug("seen " + key + " size:" + buf.length + " and send to " + remoteAddr + ":" + remotePort);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, remoteAddr, remotePort);
		server.send(packet);
	}

	protected static void write(OutputStream out, byte[] data, AtomicBoolean flag) {
		try {
			out.write(data);
		} catch (IOException ignored) {} finally {
			flag.set(true);
		}
	}
}
