package net.butfly.bus.utils.gap;

import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.IOs;
import net.butfly.albacore.utils.parallel.Concurrents;
import org.apache.ftpserver.ftplet.FtpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Config(value = "utils-gap/util-gap-kcptun-ftp/src/main/resources/dispatcher-default.properties", prefix = "bus.gap.dispatcher")
public class Dispatcher extends FtpWharf {

    private final DatagramSocket server; // virtual udp server, as kcptun low level server
	private InetAddress remoteAddress;
	private int remotePort;

	public static void main(String[] args) throws Exception {

		int dispatcherPort = Integer.parseInt(Configs.get("kcp.port", "29990"));
		String umPropFile = Configs.get("ftp.server.properties.filename");
		String ftpServer = Configs.get("ftp.server");
		String ftpRemote = Configs.get("ftp.remote");
		String account = Configs.get("ftp.account");

        Dispatcher inst = new Dispatcher(dispatcherPort, umPropFile, ftpServer, ftpRemote, account);
		inst.start();
		inst.join();
	}

	protected Dispatcher(int kcptunClientRemotePort, String umPropFile, String ftpServer, String ftpRemote,
                         String ftpAccount) throws IOException, FtpException {
        super(umPropFile, ftpServer, ftpRemote, ftpAccount);
        server = new DatagramSocket(kcptunClientRemotePort, InetAddress.getByName("127.0.0.1"));
	}

	@Override
	public void run() {
		byte[] buf = new byte[UDP_DIAGRAM_MAX_LEN];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while (true) {
			try {
				server.receive(packet);
				remoteAddress = packet.getAddress();
				remotePort = packet.getPort();
				byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
				logger().debug("server receive packet [" + data.length + " bytes] from " + remoteAddress + ":" + remotePort);
				System.out.println("server receive packet [" + data.length + " bytes] from " + remoteAddress + ":" + remotePort);
				String key = UUID.randomUUID().toString();
				AtomicBoolean finished = new AtomicBoolean(false);
				touch(key, out -> write(out, data, finished));
				while (!finished.get()) {
					Concurrents.waitSleep(10);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
    public void seen(String key, InputStream in) throws IOException {
        byte[] buf = IOs.readAll(in);
        System.out.println("dispatcher seen " + key + " size: " + buf.length);
		logger().debug("seen " + key + " size:" + buf.length + " and send to " + remoteAddress + ":" + remotePort);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, remoteAddress, remotePort);
		server.send(packet);
	}

	private static void write(OutputStream out, byte[] data, AtomicBoolean flag) {
		try {
			out.write(data);
		} catch (IOException ignored) {} finally {
			flag.set(true);
		}
	}
}
