package net.butfly.bus.serialize;

import java.util.HashMap;

import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.albacore.utils.UtilsBase;

public final class Serializers extends UtilsBase {
	@SuppressWarnings("unchecked")
	public static HashMap<String, Serializer> buildDefaultSerializerMap(Class<? extends Serializer>[] serializerClasses) {
		HashMap<String, Serializer> map = new HashMap<String, Serializer>();

		if (null == serializerClasses || serializerClasses.length == 0)
			serializerClasses = (Class<? extends Serializer>[]) ReflectionUtils.getSubClasses(Serializer.class, "").toArray();

		for (Class<? extends Serializer> clazz : serializerClasses) {
			try {
				Serializer serialize = clazz.newInstance();
				for (String mime : serialize.getSupportedMimeTypes())
					map.put(mime, serialize);
			} catch (Exception e) {}
		}
		return map;
	}
}
