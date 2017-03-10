package net.butfly.bus.utils.gap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

import net.butfly.albacore.utils.logger.Loggable;

public interface Toucher extends Loggable {
	long touch(String key, Function<OutputStream, Long> outputing) throws IOException;
}
