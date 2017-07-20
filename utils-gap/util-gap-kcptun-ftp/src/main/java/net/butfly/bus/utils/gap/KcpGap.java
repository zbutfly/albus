package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Configs.Config;

@Config(value = "bus-gap.properties", prefix = "bus.gap")
public class KcpGap extends KcpWaiter {
	public static void main(String[] args) throws IOException, InterruptedException {
		help();
		int gapPort = Integer.parseInt(args.length > 0 ? args[0] : Configs.get("port"));
		List<Path> paths = parse(args);
		KcpGap inst = new KcpGap(gapPort, paths.remove(0), paths);
		inst.start();
		inst.join();
	}

	protected KcpGap(int gapPort, Path dest, List<Path> srcs) throws IOException {
		super(EXT_REQ, EXT_RESP, gapPort, dest, srcs.toArray(new Path[srcs.size()]));
		logger().info("Start gap@kcp, listen (watch): [" + srcs.toString() + "], dump: [" + dest.toString() + "]"
				+ "]\n\tListen for diagrams from Invoker:kcp-server|Dispatcher:kcp-client on port [" + gapPort + "].");
	}
}
