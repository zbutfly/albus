package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Configs.Config;
import net.butfly.bus.utils.udp.UdpWaiter;

@Config(value = "bus-gap-dispatcher.properties", prefix = "bus.gap.dispatcher")
public class Dispatcher extends UdpWaiter {
	public static void main(String[] args) throws IOException, InterruptedException {
		help();
		int gapPort = Integer.parseInt(args.length > 0 ? args[0] : Configs.get("port"));
		List<Path> paths = parse(args);
		Dispatcher inst = new Dispatcher(gapPort, paths.remove(0), paths);
		inst.start();
		inst.join();
	}

	protected Dispatcher(int gapPort, Path dest, List<Path> srcs) throws IOException {
		super(EXT_RESP, EXT_REQ, gapPort, dest, srcs.toArray(new Path[srcs.size()]));
		logger().info("Start gap@kcp dispatcher, listen (watch): [" + srcs.toString() + "], dump: [" + dest.toString()
				+ "]\n\tListen for diagrams from Invoker:kcp-server|Dispatcher:kcp-client on port [" + gapPort + "].");
	}
}
