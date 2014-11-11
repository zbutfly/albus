package net.butfly.bus.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public interface Serializer {
	byte[] serialize(Object obj);

	<T> T deserialize(byte[] data, Type... types);

	void write(OutputStream os, Object obj) throws IOException;

	<T> T read(InputStream is, Type... types) throws IOException;

	void readThenWrite(InputStream is, OutputStream os, Type... type) throws IOException;

	boolean supportClass();
}
