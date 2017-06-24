package net.butfly.bus.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

import net.butfly.albacore.utils.logger.Loggable;

public interface Waiter extends Loggable, Runnable {
	void watch(Path from);

	void touch(String filename, Consumer<OutputStream> outputing) throws IOException;
}
