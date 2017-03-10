package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.sun.nio.file.ExtendedOpenOption;

import net.butfly.albacore.utils.logger.Loggable;

@SuppressWarnings("restriction")
public interface TunnelOstium extends Loggable {
	void seen(UUID key, InputStream data);

	default void watch(Path from) {
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
}
