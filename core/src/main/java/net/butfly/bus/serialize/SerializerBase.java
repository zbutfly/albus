package net.butfly.bus.serialize;

<<<<<<< HEAD
import java.lang.reflect.Type;
=======
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
import java.nio.charset.Charset;

public abstract class SerializerBase implements Serializer {
	protected Charset charset;

	public SerializerBase(Charset charset) {
		this.charset = null == charset ? Serializers.DEFAULT_CHARSET : charset;
	}

<<<<<<< HEAD
	public Object[] deserialize(byte[] data) {
		return this.deserialize(data, new Type[0]);
	}

=======
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
	@Override
	public String defaultMimeType() {
		return this.supportedMimeTypes()[0];
	};

	@Override
	public Charset charset() {
		return charset;
	}
}
