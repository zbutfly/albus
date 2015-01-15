package net.butfly.bus.serialize;

import java.nio.charset.Charset;

public abstract class SerializerBase implements Serializer {
	@Override
	public String getDefaultMimeType() {
		return this.getSupportedMimeTypes()[0];
	};

	@Override
	public Charset getCharset() {
		return Charset.defaultCharset();
	};
}
