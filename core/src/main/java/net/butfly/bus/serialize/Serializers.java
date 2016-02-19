package net.butfly.bus.serialize;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Charsets;

import net.butfly.albacore.utils.Instances;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.Utils;
import net.butfly.albacore.utils.async.Task;

public final class Serializers extends Utils {
	public static final String DEFAULT_MIME_TYPE = "text/plain";
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
	public static final Class<? extends Serializer> DEFAULT_SERIALIZER_CLASS = scanDefaultSerializer();

	public static Class<? extends Serializer> serializerClass(String mimeType) {
		return Instances.fetch(new Task.Callable<Map<String, Class<? extends Serializer>>>() {
			@Override
			public Map<String, Class<? extends Serializer>> call() {
				Map<String, Class<? extends Serializer>> map = new HashMap<String, Class<? extends Serializer>>();
				for (Class<? extends Serializer> subClass : Reflections.getSubClasses(Serializer.class)) {
					if (Modifier.isAbstract(subClass.getModifiers())) continue;
					Serializer def = Instances.fetch(subClass, Serializers.DEFAULT_CHARSET);
					for (String mime : def.supportedMimeTypes())
						map.put(mime, subClass);
				}
				return map;
			}
		}).get(mimeType);
	}

	public static Serializer serializer(final Class<? extends Serializer> serializerClass, final Charset charset) {
		return Instances.fetch(serializerClass, charset);
	}

	private static Class<? extends Serializer> scanDefaultSerializer() {
		for (Class<? extends Serializer> cl : Reflections.getSubClasses(Serializer.class)) {
			if (Modifier.isAbstract(cl.getModifiers()) || cl.getName().equals("net.butfly.bus.serialize.JSONSerializer")
					|| cl.getName().equals("net.butfly.bus.serialize.HessianSerializer")
					|| cl.getName().equals("net.butfly.bus.serialize.BurlapSerializer")) continue;
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
