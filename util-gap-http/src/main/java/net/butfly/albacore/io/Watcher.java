package net.butfly.albacore.io;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.butfly.albacore.utils.IOs;
import net.butfly.albacore.utils.logger.Loggable;

public class Watcher extends Thread implements Loggable {
	private static final Map<FileSystem, WatchService> watchers = new ConcurrentHashMap<>();
	// private static final Logger logger = Logger.getLogger(Watcher.class);
	private final Path dest;
	private final WatchService watcher;
	private final Set<WatchEvent.Kind<?>> events;
	private final Consumer<Path> handler;
	private final String ext;

	public Watcher(Consumer<Path> using, String ext) throws IOException {
		this(using, ext, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
	}

	/**
	 * @param ext
	 *            with "."
	 * @throws IOException
	 */
	public Watcher(Consumer<Path> using, String ext, WatchEvent.Kind<?> watching, WatchEvent.Kind<?>... watchings) throws IOException {
		this(using, Paths.get(""), ext, watching, watchings);
	}

	public Watcher(Consumer<Path> using, Path path, String ext, WatchEvent.Kind<?> watching, WatchEvent.Kind<?>... watchings)
			throws IOException {
		super();
		dest = IOs.mkdirs(path);
		logger().info("Directory [" + path + "] is being watched.");
		setName("FileWater:" + dest.toAbsolutePath());
		setDaemon(true);
		this.ext = ext;
		this.handler = using;
		events = new HashSet<>();
		events.add(watching);
		for (WatchEvent.Kind<?> w : watchings)
			events.add(w);
		watcher = watchers.computeIfAbsent(dest.getFileSystem(), fs -> {
			try {
				return fs.newWatchService();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		if (watcher == null) throw new IOException("Watcher could not be initialized.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		WatchKey key;
		try {
			key = dest.register(watcher, events.toArray(new WatchEvent.Kind<?>[events.size()]));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		while (true) {
			try {
				key = watcher.take();
			} catch (InterruptedException e) {
				break;
			}
			key.pollEvents().parallelStream().filter(ev -> events.contains(ev.kind())).map(ev -> ((WatchEvent<Path>) ev).context()).filter(
					p -> p.toString().endsWith(ext)).map(dest::resolve).forEach(handler);
			if (!key.reset()) break;
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		new Watcher(f -> System.out.println(f.toAbsolutePath()), ".txt", ENTRY_CREATE).join();
	}

	public void joining() {
		try {
			join();
		} catch (InterruptedException e) {}
	}
}
