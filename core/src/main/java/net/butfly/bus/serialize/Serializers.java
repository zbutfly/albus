package net.butfly.bus.serialize;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Map;

import net.butfly.albacore.utils.Instances;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.Utils;

import com.google.common.base.Charsets;

public final class Serializers extends Utils {
	public static final String DEFAULT_MIME_TYPE = "text/plain";
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

	public static void build(Map<String, Class<? extends Serializer>> map) {
		for (Class<? extends Serializer> subClass : Reflections.getSubClasses(Serializer.class))
			if (!Modifier.isAbstract(subClass.getModifiers())) {
				Serializer def = Reflections.construct(subClass,
						Reflections.parameter(Serializers.DEFAULT_CHARSET, Charset.class));
				for (String mime : def.supportedMimeTypes())
					map.put(mime, subClass);
			}
	}

	public static Serializer serializer(final Class<? extends Serializer> serializerClass, final Charset charset) {
		return Instances.fetch(new Instances.Instantiator<Serializer>() {
			@Override
			public Serializer create() {
				return Reflections.construct(serializerClass, Reflections.parameter(charset, Charset.class));
			}
		}, serializerClass, charset);
	}
}
