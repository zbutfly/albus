package net.butfly.bus.serialize;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.Utils;

import com.google.common.base.Charsets;

public final class Serializers extends Utils {
	public static final String DEFAULT_MIME_TYPE = "text/plain";
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

	private static final Map<String, Class<? extends Serializer>> CLASSES = new HashMap<String, Class<? extends Serializer>>();
	private static final Map<String, Serializer> INSTANCES = new HashMap<String, Serializer>();
	static {
		build();
	}

	private static void build() {
		for (Class<? extends Serializer> subClass : Reflections.getSubClasses(Serializer.class))
			if (!Modifier.isAbstract(subClass.getModifiers())) {
				Serializer def = Reflections.construct(subClass,
						Reflections.parameter(Serializers.DEFAULT_CHARSET, Charset.class));
				for (String mime : def.supportedMimeTypes())
					CLASSES.put(mime, subClass);
			}
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
		Class<? extends Serializer> subClass = CLASSES.get(mimeType);
		if (null == subClass) throw new RuntimeException("mimeType not supportted: " + mimeType);
		String key = subClass.getName() + "#" + charset.name();
		Serializer inst = INSTANCES.get(key);
		if (null == inst) {
			inst = Reflections.construct(subClass, Reflections.parameter(charset, Charset.class));
			INSTANCES.put(key, inst);
		}
		return inst;
	}

	public static Serializer serializer(Class<? extends Serializer> clazz) {
		Serializer def = Reflections.construct(clazz, Reflections.parameter(Serializers.DEFAULT_CHARSET, Charset.class));
		return serializer(def.defaultMimeType());
	}
}
