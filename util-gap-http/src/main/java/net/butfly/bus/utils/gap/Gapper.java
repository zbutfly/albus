package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.nio.file.ExtendedOpenOption;

import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import net.butfly.albacore.utils.Configs;
import net.butfly.albacore.utils.logger.Logger;

@SuppressWarnings("restriction")
public abstract class Gapper {
	protected final Logger logger;
	private final String confPrefix;
	protected final String host;
	protected final int port;
	protected final Path src, dst;

	protected final Map<UUID, byte[]> sessions = new ConcurrentHashMap<>();

	protected Gapper(String conf, String confPrefix) {
		this.logger = Logger.getLogger(getClass());
		this.confPrefix = confPrefix;
		Configs.setConfig(conf);
		host = conf("host", "0.0.0.0");
		port = Integer.parseInt(conf("port", "80"));
		src = Paths.get(conf("src", "./pool"));
		dst = Paths.get(conf("src", "./pool"));
	}

	protected String conf(String key, String def) {
		return Configs.MAIN_CONF.getOrDefault(confPrefix + key, def);
	}

	protected byte[] request(HttpString method, String path, String queryString, HeaderMap headers, Map<String, Cookie> cookies,
			Receiver recv) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void response(HttpServerExchange exch, byte[] resp) {
		// TODO Auto-generated method stub
	}

	protected void watch(Path path) {
		String fname = path.getFileName().toString();
		Path working = path.getParent().resolve(fname + ".working");
		byte[] data;
		try {
			Files.move(path, working, StandardCopyOption.ATOMIC_MOVE);
			data = new byte[(int) Files.size(working)];
			try (InputStream is = Files.newInputStream(working, ExtendedOpenOption.NOSHARE_READ)) {
				is.read(data);
			} finally {
				Files.move(working, path.getParent().resolve(fname + ".finished"), StandardCopyOption.ATOMIC_MOVE);
			}
		} catch (IOException e) {
			logger.error("File read fail on [" + path.toAbsolutePath().toString() + "]", e);
			return;
		}
		UUID key = UUID.fromString(fname.substring(fname.lastIndexOf(".")));
		sessions.put(key, data);
		logger.debug("Data read [" + data.length + " bytes], now pool: " + sessions.size());
	}
}
