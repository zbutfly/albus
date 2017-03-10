package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

import com.sun.nio.file.ExtendedOpenOption;

import net.butfly.albacore.utils.logger.Loggable;

/**
 * 这是一个网闸
 * 
 * @author butfly
 */
@SuppressWarnings("restriction")
public class GapTunnel implements Loggable {
	protected final Path dumpDst;

	public GapTunnel(Path dumpDst) throws IOException {
		this.dumpDst = dumpDst;
	}

	public long writing(String filename, Function<OutputStream, Long> outputing) throws IOException {
		Path working = dumpDst.resolve(filename + ".working"), worked = dumpDst.resolve(filename);
		try (OutputStream os = Files.newOutputStream(working, ExtendedOpenOption.NOSHARE_WRITE);) {
			long l = outputing.apply(os);
			logger().debug("Data write [" + filename + "]:[" + l + " bytes].");
			return l;
		} finally {
			Files.move(working, worked, StandardCopyOption.ATOMIC_MOVE);
		}
	}
}
