package net.butfly.bus.utils.gap;

import static net.butfly.albacore.utils.Configs.get;

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
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sun.nio.file.ExtendedOpenOption;

import net.butfly.albacore.io.Streams;
import net.butfly.albacore.io.Watcher;
import net.butfly.albacore.utils.Systems;
import net.butfly.bus.utils.Waiter;

/**
 * 这是网闸上的一个通道（口）
 * 
 * @author butfly
 */
@SuppressWarnings("restriction")
public abstract class WaiterImpl extends Thread implements Waiter {
	protected final String host;
	protected final int port;
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
		host = get("host", clis[0], "0.0.0.0");
		port = Integer.parseInt(get("port", clis[1], "80"));
		dumpDest = Paths.get(get("dst", clis[2], "./pool"));
		String cmds = Arrays.stream(Arrays.copyOfRange(clis, 2, clis.length)).filter(Streams.NOT_NULL).collect(Collectors.joining("\n"));
		Path[] dsts = Arrays.stream(get("src", cmds.length() == 0 ? "./pool" : cmds).split("\n")).map(Paths::get).collect(Collectors
				.toList()).toArray(new Path[0]);
		if (dsts.length > 1) logger().error("Multiple path to be watching defined but only support first [" + dsts[0] + "] now....");
		watcher = new Watcher(this::watch, dsts[0], watchExt, StandardWatchEventKinds.ENTRY_CREATE);
		logger().info("Starting on [" + host + ":" + port + "]");
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
				if (hp[0].length() > 0) r.set(0, hp[0]);
				if (hp[1].length() > 0) r.set(1, hp[1]);
			} else if (hp[0].length() > 0) r.set(1, hp[0]);
			if (args.length > 1) for (int i = 1; i < args.length; i++)
				r.set(i + 1, args[i]);
			return r.toArray(new String[r.size()]);
		}
	}

	abstract void seen(UUID key, InputStream in);

	@Override
	public void watch(Path from) {
		String fname = from.getFileName().toString();
		UUID key = UUID.fromString(fname.substring(0, fname.lastIndexOf(".")));
		Path working = from.getParent().resolve(fname + ".working");
		try {
			Files.move(from, working, StandardCopyOption.ATOMIC_MOVE);
			try (InputStream is = Files.newInputStream(working, ExtendedOpenOption.NOSHARE_READ)) {
				seen(key, is);
			} finally {
				Files.move(working, from.getParent().resolve(fname + ".finished"), StandardCopyOption.ATOMIC_MOVE);
			}
		} catch (IOException e) {
			logger().error("File read fail on [" + from.toAbsolutePath().toString() + "]", e);
		}
	}

	@Override
	public void touch(Path dest, String filename, Consumer<OutputStream> outputing) {
		Path working = dest.resolve(filename + ".working"), worked = dest.resolve(filename);
		try (OutputStream os = Files.newOutputStream(working, StandardOpenOption.CREATE, ExtendedOpenOption.NOSHARE_WRITE);) {
			outputing.accept(os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				Files.move(working, worked, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
