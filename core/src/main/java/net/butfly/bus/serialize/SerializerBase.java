package net.butfly.bus.serialize;

import java.nio.charset.Charset;

public abstract class SerializerBase implements Serializer {
	protected Charset charset;

	public SerializerBase(Charset charset) {
		this.charset = null == charset ? Serializers.DEFAULT_CHARSET : charset;
	}

	@Override
	public String defaultMimeType() {
		return this.supportedMimeTypes()[0];
	};

	@Override
	public Charset charset() {
		return charset;
	}
}
