package net.butfly.bus.utils.gap;

import static net.butfly.albacore.utils.Configs.getn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.butfly.albacore.io.Watcher;
import net.butfly.albacore.io.utils.Streams;
import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Systems;
import net.butfly.albacore.utils.Texts;
import net.butfly.bus.utils.Waiter;

/**
 * 这是网闸上的一个通道（口）
 * 
 * @author butfly
 */
public abstract class WaiterImpl extends Thread implements Waiter {
	public static final String EXT_REQ = ".req";
	public static final String EXT_RESP = ".rep";
	public static final String EXT_WORKING = ".working";
	public static final String EXT_FINISHED = ".finished";

	protected final String host;
	protected final int port;
	protected final Set<String> methods;
	protected final String touchExt;
	protected final Path dumpDest;
	protected final Watcher watcher;

	protected WaiterImpl(String watchExt, String touchExt, String... args) throws IOException {
		super();
		setName(getClass().getSimpleName() + "Thread");
		this.touchExt = touchExt;
		String[] clis = parseArgs(args);
		System.err.println("Usage: ");
		System.err.println("\tjava " + Systems.getMainClass());
		System.err.println("\tjava " + Systems.getMainClass() + " <host:port>");
		System.err.println("\tjava " + Systems.getMainClass() + " <dumping path> <wartching path>");
		System.err.println("\tjava " + Systems.getMainClass()
				+ " <host:port> <dumping path> <wartching path(support multiple later NOT NOW...)>");
		host = getn(clis[0], "host", "0.0.0.0");
		port = Integer.parseInt(getn(clis[1], "port", "80"));
		String ms = Configs.get("method", "POST,OPTION");
		methods = Arrays.asList(ms.split(",")).parallelStream().filter(Texts::notEmpty).map(String::toUpperCase).collect(Collectors
				.toSet());
		dumpDest = Paths.get(getn(clis[2], "dst", "./pool"));
		logger().info("Starting on [" + host + ":" + port + "], allow methods: [" + ms.toString() + "] data dump to [" + dumpDest + "]");

		String watchings = Arrays.stream(Arrays.copyOfRange(clis, 3, clis.length)).filter(Streams.NOT_NULL).collect(Collectors.joining(
				"\n"));
		watchings = getn(watchings, "src", "./pool");
		Path[] watchs = Arrays.stream(watchings.split("\n")).map(Paths::get).toArray(i -> new Path[i]);
		if (watchs.length > 1) logger().error("Multiple path to be watching defined but only support first [" + watchs[0] + "] now....");
		watcher = new Watcher(this::watch, watchs[0], watchExt, StandardWatchEventKinds.ENTRY_CREATE);
		watcher.start();
	}

	/**
	 * @param args
	 * @return HOST, PORT, DESTING, WATCHING...
	 */
	private String[] parseArgs(String[] args) {
		switch (args.length) {
		case 0:
			return new String[4];
		case 2:
			return new String[] { null, null, args[0], args[1] };
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
			return r.toArray(new String[r.size()]);
		}
	}

	abstract void seen(UUID key, InputStream in);

	@Override
	public void watch(Path from) {
		String fname = from.getFileName().toString();
		UUID key = UUID.fromString(fname.substring(0, fname.lastIndexOf(".")));
		Path working = from.getParent().resolve(fname + EXT_WORKING);
		try {
			Files.move(from, working, StandardCopyOption.ATOMIC_MOVE);
			try (InputStream is = Files.newInputStream(working, StandardOpenOption.READ)) {
				seen(key, is);
			} finally {
				Files.move(working, from.getParent().resolve(fname + EXT_FINISHED), StandardCopyOption.ATOMIC_MOVE);
			}
		} catch (IOException e) {
			logger().error("File read fail on [" + from.toAbsolutePath().toString() + "]", e);
		}
	}

	@Override
	public void touch(String filename, Consumer<OutputStream> outputing) throws IOException {
		Path working = dumpDest.resolve(filename + EXT_WORKING), worked = dumpDest.resolve(filename);
		try (OutputStream os = Files.newOutputStream(working, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);) {
			outputing.accept(os);
		} finally {
			Files.move(working, worked, StandardCopyOption.ATOMIC_MOVE);
			logger().debug(() -> {
				long s = 0;
				try {
					s = Files.size(worked);
				} catch (IOException e) {}
				return "Data saved: [" + filename + "], size: [" + s + "].";
			});
		}
	}
}
