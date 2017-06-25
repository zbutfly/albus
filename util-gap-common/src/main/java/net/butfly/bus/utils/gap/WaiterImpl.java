package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.util.function.Consumer;

import net.butfly.albacore.io.Watcher;
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

	protected final String touchExt;
	protected final Path dumpDest;
	protected final Watcher watcher;

	protected WaiterImpl(String watchExt, String touchExt, Path dumpDest, Path... watchs) throws IOException {
		super();
		setName(getClass().getSimpleName() + "Thread");
		this.touchExt = touchExt;
		this.dumpDest = dumpDest;
		watcher = new Watcher(this::watch, watchs[0], watchExt, StandardWatchEventKinds.ENTRY_CREATE);
		watcher.start();
	}

	protected abstract void seen(String key, InputStream in) throws IOException;

	@Override
	public void watch(Path from) {
		String fname = from.getFileName().toString();
		String key = fname.substring(0, fname.lastIndexOf("."));
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
