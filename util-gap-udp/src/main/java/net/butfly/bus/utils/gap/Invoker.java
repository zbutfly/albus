package net.butfly.bus.utils.gap;

import net.butfly.albacore.utils.Configs.Config;
import net.butfly.bus.utils.udp.UdpClient;
import net.butfly.bus.utils.udp.UdpServer;
import net.butfly.bus.utils.udp.UdpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.UUID;

/**
 * 这是内网口
 * 1. 负责把文件解析成数据包并发送请求
 * 2. 负责把返回的数据包写成文件，若超过timeout仍无响应，则返回一个空包以继续处理下一个
 * @author butfly
 */
@Config(value = "/net/butfly/bus/gap/udp/udp-invoker-default.properties", prefix = "bus.gap.udp.invoker")
public class Invoker extends WaiterImpl {
	private final UdpServer server;
	private final UdpClient client;

	public static void main(String[] args) throws IOException, InterruptedException {
		Invoker inst = new Invoker(args);
		inst.start();
		inst.join();
	}

	protected Invoker(String... args) throws IOException {
		super(EXT_REQ, EXT_RESP, args);
		server = new UdpServer(host, listenPort, UdpUtils.UDP_SERVER_TIMEOUT);
		client = new UdpClient();
	}

	@Override
	public void seen(UUID key, InputStream in) {
        DatagramPacket packet = UdpUtils.load(in);
        UdpUtils.request(packet, pkt -> {
            client.send(UdpUtils.redirect(pkt, host, responsePort));
            byte[] buf = new byte[UdpUtils.UDP_PACKET_SIZE];

            DatagramPacket respPacket = new DatagramPacket(buf, buf.length);
            try {
                server.receive(respPacket);
            } catch (SocketTimeoutException e) {
                respPacket.setAddress(packet.getAddress());
                respPacket.setPort(packet.getPort());
                logger().error("udp packet response timeout");
            }
            try {
                touch(dumpDest, key.toString() + touchExt, out -> UdpUtils.save(out, pkt));
            } catch (IOException e) {
                logger().error("udp packet save failed");
            }

        });
	}

	@Override
	public void run() {
		watcher.joining();
	}
}
