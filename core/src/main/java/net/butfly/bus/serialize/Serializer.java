package net.butfly.bus.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public interface Serializer {
	byte[] serialize(Object obj);

<<<<<<< HEAD
	Object[] deserialize(byte[] data, Type[] types);

	<T> T deserialize(byte[] data, Type type);

	Object[] deserialize(byte[] data);

	void write(OutputStream os, Object obj) throws IOException;

	<T> T read(InputStream is, Type types) throws IOException;

	Object[] read(InputStream is, Type[] types) throws IOException;
=======
	<T> T deserialize(byte[] data, Type... types);

	void write(OutputStream os, Object obj) throws IOException;

	<T> T read(InputStream is, Type... types) throws IOException;
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491

	void readThenWrite(InputStream is, OutputStream os, Type... type) throws IOException;

	boolean supportClass();

<<<<<<< HEAD
	<T> T fromString(String str, Type type);

	Object[] fromString(String str, Type[] types);
=======
	<T> T fromString(String str, Type... types);
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491

	String asString(Object obj);

	String[] supportedMimeTypes();

	String defaultMimeType();

	Charset charset();
<<<<<<< HEAD

=======
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
}
