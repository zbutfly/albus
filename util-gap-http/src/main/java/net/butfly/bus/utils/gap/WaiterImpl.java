package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.util.UUID;
import java.util.function.Function;

import com.sun.nio.file.ExtendedOpenOption;

import net.butfly.albacore.io.Watcher;
import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.Configs.Conf;
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
	protected final Conf conf;

	protected WaiterImpl(String watchExt, String touchExt) throws IOException {
		super();
		setName(getClass().getSimpleName() + "Thread");
		conf = Configs.MAIN;
		host = conf.get("host", "0.0.0.0");
		port = Integer.parseInt(conf.get("port", "80"));
		this.touchExt = touchExt;
		dumpDest = Paths.get(conf.get("src", "./pool"));
		watcher = new Watcher(this::watch, Paths.get(conf.get("src", "./pool")), watchExt, StandardWatchEventKinds.ENTRY_CREATE);
		logger().info("Starting on [" + host + ":" + port + "]");
		watcher.start();
	}

	abstract void seen(UUID key, InputStream data);

	@Override
	public void watch(Path from) {
		String fname = from.getFileName().toString();
		UUID key = UUID.fromString(fname.substring(fname.lastIndexOf(".")));
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
	public long touch(Path dest, String filename, Function<OutputStream, Long> outputing) throws IOException {
		Path working = dest.resolve(filename + ".working"), worked = dest.resolve(filename);
		try (OutputStream os = Files.newOutputStream(working, ExtendedOpenOption.NOSHARE_WRITE);) {
			long l = outputing.apply(os);
			logger().debug("Data write [" + filename + "]:[" + l + " bytes].");
			return l;
		} finally {
			Files.move(working, worked, StandardCopyOption.ATOMIC_MOVE);
		}
	}
}
