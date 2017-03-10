package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.util.UUID;

import com.sun.nio.file.ExtendedOpenOption;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import net.butfly.albacore.utils.parallel.Concurrents;

@SuppressWarnings("restriction")
public class Dispatcher extends Gapper {
	// private final Watcher watcher;

	protected Dispatcher(String conf) throws IOException {
		super(conf, "bus.gap.dispatcher.");
		logger.info("GAP-Dispatcher start on [" + host + ":" + port + "]");
		new Watcher(this::watch, src, ".resp", StandardWatchEventKinds.ENTRY_CREATE);
		Undertow.builder().addHttpListener(port, host).setHandler(exch -> {
			UUID key = UUID.randomUUID();
			logger.trace(exch.toString());
			write(key, exch);
			byte[] resp;
			while ((resp = sessions.remove(key)) == null)
				Concurrents.waitSleep();
			response(exch, resp);
		}).build().start();
	}

	protected void write(UUID key, HttpServerExchange exch) throws IOException {
		Path working = src.resolve(key + ".req.working"), worked = src.resolve(key + ".req");
		try (OutputStream os = Files.newOutputStream(src.resolve(key + ".req.working"), ExtendedOpenOption.NOSHARE_WRITE);) {
			os.write(request(exch.getRequestMethod(), exch.getRequestPath(), exch.getQueryString(), exch.getRequestHeaders(), exch
					.getRequestCookies(), exch.getRequestReceiver()));
		}
		Files.move(working, worked, StandardCopyOption.ATOMIC_MOVE);
	}

	public static void main(String[] args) throws IOException {
		new Dispatcher(args.length < 1 ? "bus-gap-dispatcher.properties" : args[0]);
	}
}
