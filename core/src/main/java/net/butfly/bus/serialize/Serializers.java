package net.butfly.bus.serialize;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.Instances;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.Utils;
import net.butfly.albacore.utils.async.Task;

import com.google.common.base.Charsets;

public final class Serializers extends Utils {
	public static final String DEFAULT_MIME_TYPE = "text/plain";
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

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
}
