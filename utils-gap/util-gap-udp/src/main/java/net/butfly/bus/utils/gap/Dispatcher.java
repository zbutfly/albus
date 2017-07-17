package net.butfly.bus.utils.gap;

import net.butfly.albacore.utils.Configs.Config;
import net.butfly.albacore.utils.parallel.Concurrents;
import net.butfly.bus.utils.udp.UdpClient;
import net.butfly.bus.utils.udp.UdpServer;
import net.butfly.bus.utils.udp.UdpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 这是外网口
 * 1. 负责接收UDP数据包，并写文件
 * 2. 负责解析响应的数据包，并发送到指定端口
 * @author butfly
 */
@Config(value = "net/butfly/bus/gap/udp/udp-dispatcher-default.properties", prefix = "bus.gap.udp.dispatcher")
public class Dispatcher extends WaiterImpl {
	private final UdpServer server;
	private final UdpClient client;
	private final Map<UUID, Consumer<DatagramPacket>> handlers;

	public static void main(String[] args) throws IOException, InterruptedException {
		Dispatcher inst = new Dispatcher(args);
		inst.start();
		inst.join();
	}

	protected Dispatcher(String... args) throws IOException {
		super(EXT_RESP, EXT_REQ, args);
		handlers = new ConcurrentHashMap<>();
		client = new UdpClient();
		server = new UdpServer(host, listenPort).setHandler(this::handle);
	}

	private void handle(DatagramPacket packet) {
        UUID key = UUID.randomUUID();
        logger().trace(UdpUtils.bytesToHex(packet.getData()));
		AtomicBoolean finished = new AtomicBoolean(false);
        if (null != handlers.putIfAbsent(key, resp -> finished.set(client.send(UdpUtils.redirect(resp, host, responsePort))))) {
            logger().error("Resp/Req key [" + key + "] duplicated, current request lost...");
        } else {
            logger().debug("Request [" + key + "] arrive, pool size: " + handlers.size());
			try {
				touch(dumpDest, key.toString() + touchExt, out -> UdpUtils.save(out, packet));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
            while (!finished.get())
                Concurrents.waitSleep(10);
            logger().debug("Response [" + key + "] sent.");
        }
    }

    /**
     * dispatcher端监视到新文件时的处理，转换为Udp数据包并发送出去
     * @param key 与文件名对应的UUID
     * @param data 从文件中读取到的数据
     */
	@Override
	public void seen(UUID key, InputStream data) {
        Consumer<DatagramPacket> h = handlers.remove(key);
		if (null != h) {
			logger().debug("Response [" + key + "] left, pool size: " + handlers.size());
			h.accept(UdpUtils.load(data));
		} else logger().error("Resp/Req key [" + key + "] not found, current response lost...");
	}

	@Override
	public void run() {
		server.start();
	}
}
