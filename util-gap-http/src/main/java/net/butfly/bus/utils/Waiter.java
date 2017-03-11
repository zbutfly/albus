package net.butfly.bus.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Function;

import net.butfly.albacore.utils.logger.Loggable;

public interface Waiter extends Loggable, Runnable {
	void watch(Path from);

	long touch(Path dest, String filename, Function<OutputStream, Long> outputing) throws IOException;
}
