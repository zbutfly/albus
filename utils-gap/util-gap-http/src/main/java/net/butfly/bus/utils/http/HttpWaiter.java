package net.butfly.bus.utils.http;

import static net.butfly.albacore.utils.Configs.getn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.butfly.albacore.io.utils.Streams;
import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Systems;
import net.butfly.albacore.utils.Texts;
import net.butfly.bus.utils.gap.WaiterImpl;

public abstract class HttpWaiter extends WaiterImpl {
	protected final InetSocketAddress addr;
	protected final Set<String> methods;

	protected HttpWaiter(String watchExt, String touchExt, HttpWaiterConfig config) throws IOException {
		super(watchExt, touchExt, config.dest, config.watchs);
		this.addr = config.addr;
		this.methods = config.methods;
	}

	/**
	 * @param args
	 * @return
	 */
	protected static HttpWaiterConfig parseArgs(String... args) {
		System.err.println("Usage: ");
		System.err.println("\tjava " + Systems.getMainClass());
		System.err.println("\tjava " + Systems.getMainClass() + " <host:port>");
		System.err.println("\tjava " + Systems.getMainClass() + " <dumping path> <wartching path>");
		System.err.println("\tjava " + Systems.getMainClass()
				+ " <host:port> <dumping path> <wartching path(support multiple later NOT NOW...)>");
		String[] clis;
		switch (args.length) {
		case 0:
			clis = new String[4];
			break;
		case 2:
			clis = new String[] { null, null, args[0], args[1] };
			break;
		default: // 1 or more than 3
			List<String> r = new ArrayList<>();
			String[] hp = args[0].split(":", 2);
			if (hp.length == 2) {
				r.add(Texts.orNull(hp[0]));
				r.add(Texts.orNull(hp[1]));
			} else {
				r.add(null);
				r.add(Texts.orNull(hp[0]));
			}
			if (args.length > 1) for (int i = 1; i < args.length; i++)
				r.add(args[i]);
			clis = r.toArray(new String[r.size()]);
		}
		Path dest = Paths.get(getn(clis[2], "dst", "./pool"));

		InetSocketAddress addr = new InetSocketAddress(getn(clis[0], "host", "0.0.0.0"), Integer.parseInt(getn(clis[1], "port", "80")));
		Set<String> methods = Arrays.asList(Configs.get("method", "POST,OPTION").split(",")).parallelStream().filter(Texts::notEmpty).map(
				String::toUpperCase).collect(Collectors.toSet());

		Path[] watchs = Arrays.stream(getn(Arrays.stream(Arrays.copyOfRange(clis, 3, clis.length)).filter(Streams.NOT_NULL).collect(
				Collectors.joining("\n")), "src", "./pool").split("\n")).map(Paths::get).toArray(i -> new Path[i]);
		return new HttpWaiterConfig(dest, watchs, addr, methods);
	}

	public static class HttpWaiterConfig {
		public final Path dest;
		public final Path[] watchs;
		public final InetSocketAddress addr;
		public final Set<String> methods;

		public HttpWaiterConfig(Path dest, Path[] watchs, InetSocketAddress addr, Set<String> methods) {
			super();
			this.dest = dest;
			this.watchs = watchs;
			this.addr = addr;
			this.methods = methods;
		}
	}
}
