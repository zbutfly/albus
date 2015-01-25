package net.butfly.bus.serialize;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.UtilsBase;

import com.google.common.base.Charsets;

public final class Serializers extends UtilsBase {
	public static final String DEFAULT_MIME_TYPE = "text/plain";
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

	private static final Map<String, Class<? extends Serializer>> pool = new HashMap<String, Class<? extends Serializer>>();
	static {
		build();
	}

	private static void build() {
		for (Class<? extends Serializer> clazz : Reflections.getSubClasses(Serializer.class, ""))
			try {
				Serializer def = Reflections.construct(clazz,
						Reflections.parameters(Charset.class, Serializers.DEFAULT_CHARSET));
				for (String mime : def.supportedMimeTypes())
					pool.put(mime, clazz);
			} catch (Exception e) {}
	}

	public static Serializer serializer() {
		return serializer(DEFAULT_MIME_TYPE, DEFAULT_CHARSET);
	}

	public static Serializer serializer(Charset charset) {
		return serializer(DEFAULT_MIME_TYPE, charset);
	}

	public static Serializer serializer(String mimeType) {
		return serializer(mimeType, DEFAULT_CHARSET);
	}

	public static Serializer serializer(String mimeType, Charset charset) {
		Class<? extends Serializer> sub = pool.get(mimeType);
		if (null == sub) throw new RuntimeException("mimeType not supportted: " + mimeType);
		return Reflections.construct(sub, Reflections.parameters(Charset.class, charset));
	}

	public static Serializer serializer(Class<? extends Serializer> clazz) {
		Serializer def = Reflections.construct(clazz,
				Reflections.parameters(Charset.class, Serializers.DEFAULT_CHARSET));
		return serializer(def.defaultMimeType());
	}
}
