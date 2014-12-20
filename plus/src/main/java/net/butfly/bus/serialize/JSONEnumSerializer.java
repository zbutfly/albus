package net.butfly.bus.serialize;

import java.lang.reflect.Type;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.EnumUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

@SuppressWarnings("rawtypes")
public class JSONEnumSerializer implements JsonSerializer<Enum>, JsonDeserializer<Enum> {
	@Override
	public JsonElement serialize(Enum src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(EnumUtils.value(src));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enum<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		Class clazz = (Class) typeOfT;
		if (!clazz.isEnum()) throw new SystemException("", "only enum could be deserialized to.");
		Integer value = json.getAsInt();
		Enum<?>[] values;
		try {
			values = (Enum<?>[]) clazz.getMethod("values").invoke(null);
		} catch (Exception e) {
			throw new SystemException("", "only enum could be deserialized to.");
		}
		for (Enum<?> e : values) {
			if (EnumUtils.value(e) == value) return e;
		}
		return null;
	}
}
