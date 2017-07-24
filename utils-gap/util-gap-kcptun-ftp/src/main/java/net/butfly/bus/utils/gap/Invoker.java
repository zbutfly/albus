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
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

//@Config(value = "utils-gap/util-gap-kcptun-ftp/src/main/resources/invoker-default.properties", prefix = "bus.gap.invoker")
@Config(value = "classes/invoker-default.properties", prefix = "bus.gap.invoker")
public class Invoker extends FtpWharf {
    private final DatagramSocket client;

	public static void main(String[] args) throws Exception {
		int kcpServerPort = Integer.parseInt(Configs.get("kcp.port", "29992"));
        String umPropFile = Configs.get("ftp.server.properties.filename");
        String ftpServer = Configs.get("ftp.server");
        String ftpRemote = Configs.get("ftp.remote");
        String account = Configs.get("ftp.account");
		Invoker inst = new Invoker(kcpServerPort, umPropFile, ftpServer, ftpRemote, account);
		inst.start();
		inst.join();
	}

	protected Invoker(int gapPort, String umPropFile, String ftpServer, String ftpRemote,
					  String ftpAccount) throws Exception {
		super(umPropFile, ftpServer, ftpRemote, ftpAccount);
		client = new DatagramSocket();
		client.connect(new InetSocketAddress("127.0.0.1", gapPort));
	}

	@Override
	public void seen(String key, InputStream in) throws IOException {
		byte[] data = IOs.readAll(in);
		DatagramPacket packet = new DatagramPacket(data, data.length);
		logger().debug("seen [{} bytes] data with key {} and send to {}:{}.", data.length, key, client.getInetAddress().getHostAddress(), client.getPort());
		client.send(packet);
	}

	@Override
	public void run() {
		byte[] buf = new byte[UDP_DIAGRAM_MAX_LEN];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while (true) {
			try {
				client.receive(packet);
				logger().debug("kcptun receive [{} bytes] from {}:{}.", packet.getLength(), packet.getAddress(), packet.getPort());
				String key = UUID.randomUUID().toString();
				AtomicBoolean finished = new AtomicBoolean(false);
				touch(key, out -> write(out, packet.getData(), packet.getLength(), finished));
				while (!finished.get()) Concurrents.waitSleep(10);
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
