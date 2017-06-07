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
	protected final int listenPort;
	protected final int responsePort;
	/**生成的文件的拓展名*/
	protected final String touchExt;
	/**生成文件的目标目录*/
	protected final Path dumpDest;
	protected final Watcher watcher;

	/**
	 *
	 * @param watchExt 被watch的文件扩展名
	 * @param touchExt 要生成的文件的扩展名
	 * @param args 其它参数
	 * @throws IOException
	 */
	protected WaiterImpl(String watchExt, String touchExt, String... args) throws IOException {
		super();
		setName(getClass().getSimpleName() + "Thread");
		this.touchExt = touchExt;
		String[] clis = parseArgs(args);
		System.err.println("Usage: ");
		System.err.println("\tjava " + Systems.getMainClass());
//		System.err.println("\tjava " + Systems.getMainClass() + " <dumping path> <watching path>");
		System.err.println("\tjava " + Systems.getMainClass() + " <host> <listen port> <response port>");
		System.err.println("\tjava " + Systems.getMainClass()
				+ " <host> <listen port> <response port> <dumping path> <watching path(support multiple later NOT NOW...)>");

		for (int i = 0; i < clis.length; i++) {
			System.err.println("clis[" + i + "]:" + clis[i]);
		}
		host = getn(clis[0], "host", "0.0.0.0");
		listenPort = Integer.parseInt(getn(clis[1], "listen.port", "6000"));
		responsePort = Integer.parseInt(getn(clis[2], "response.port", "6002"));
		dumpDest = Paths.get(getn(clis[3], "dst", "./pool"));

		/*关注的目录*/
		String watchings = Arrays.stream(Arrays.copyOfRange(clis, 4, clis.length)).filter(Streams.NOT_NULL).collect(Collectors.joining(
				"\n"));
		watchings = getn(watchings, "src", "./pool");
		Path[] watchs = Arrays.stream(watchings.split("\n")).map(Paths::get).toArray(i -> new Path[i]);
		if (watchs.length > 1) logger().error("Multiple path to be watching defined but only support first [" + watchs[0] + "] now....");
		watcher = new Watcher(this::watch, watchs[0], watchExt, StandardWatchEventKinds.ENTRY_CREATE);
		watcher.start();
	}

	/**
	 * @param args
	 * @return src, dst, host, listenPort, responsePort
	 */
	private String[] parseArgs(String[] args) {
		switch (args.length) {
//		case 2:
//			return new String[] {null, null, null, args[0], args[1]};
		case 3:
			return new String[] {args[0], args[1], args[2], null, null};
		case 5:
			return args;
		default:
			logger().error("Invalid parameters, use default");
			return new String[5];
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
	public void touch(Path dest, String filename, Consumer<OutputStream> outputting) throws IOException {
		Path working = dest.resolve(filename + EXT_WORKING), worked = dest.resolve(filename);
        try (OutputStream os = Files.newOutputStream(working, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);) {
            outputting.accept(os);
        } finally {
            Files.move(working, worked, StandardCopyOption.ATOMIC_MOVE);
            logger().debug(() -> {
                long s = 0;
                try {
                    s = Files.size(worked);
                } catch (IOException e) {
                }
                return "Data saved: [" + filename + "], size: [" + s + "].";
            });
        }
    }
}
