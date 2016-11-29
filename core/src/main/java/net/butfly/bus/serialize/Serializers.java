package net.butfly.bus.serialize;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;

import net.butfly.albacore.utils.Instances;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.Utils;

public final class Serializers extends Utils {
	public static final String DEFAULT_MIME_TYPE = "text/plain";
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
	public static final Class<? extends Serializer> DEFAULT_SERIALIZER_CLASS = scanDefaultSerializer();

	public static Serializer serializer(String mimeType, Charset charset) {
		return Instances.fetch(() -> {
			for (Class<? extends Serializer> subClass : Reflections.getSubClasses(Serializer.class)) {
				if (Modifier.isAbstract(subClass.getModifiers())) continue;
				Serializer def = Instances.construct(subClass, charset == null ? Serializers.DEFAULT_CHARSET : charset);
				for (String mime : def.supportedMimeTypes())
					if (mimeType.equals(mime)) return def;
			}
			throw new RuntimeException("No serializer found for: " + mimeType);
		}, Serializer.class, mimeType, charset);
	}

	private static Class<? extends Serializer> scanDefaultSerializer() {
		for (Class<? extends Serializer> cl : Reflections.getSubClasses(Serializer.class)) {
			if (Modifier.isAbstract(cl.getModifiers()) || cl.getName().equals("net.butfly.bus.serialize.JSONSerializer") || cl.getName()
					.equals("net.butfly.bus.serialize.HessianSerializer") || cl.getName().equals(
							"net.butfly.bus.serialize.BurlapSerializer")) continue;
			return cl;
		}
		Class<? extends Serializer> cl = Reflections.forClassName("net.butfly.bus.serialize.HessianSerializer");
		if (null != cl) return cl;
		cl = Reflections.forClassName("net.butfly.bus.serialize.JSONSerializer");
		if (null != cl) return cl;
		cl = Reflections.forClassName("net.butfly.bus.serialize.BurlapSerializer");
		if (null != cl) return cl;
		throw new RuntimeException("Could not found any serializer class implementation in classpath.");
	}
}
