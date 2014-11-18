package net.butfly.bus.serialize;

import java.lang.reflect.Type;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.support.EnumSupport;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

@SuppressWarnings("rawtypes")
public class JSONEnumSerializer implements JsonSerializer<EnumSupport>, JsonDeserializer<EnumSupport> {
	@Override
	public JsonElement serialize(EnumSupport src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.value());
	}

	@SuppressWarnings("unchecked")
	@Override
	public EnumSupport deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		Class clazz = (Class) typeOfT;
		if (!clazz.isEnum()) throw new SystemException("", "only enum could be deserialized to.");
		Integer value = json.getAsInt();
		EnumSupport[] values;
		try {
			values = (EnumSupport[]) clazz.getMethod("values").invoke(null);
		} catch (Exception e) {
			throw new SystemException("", "only enum could be deserialized to.");
		}
		for (EnumSupport e : values) {
			if (e.value() == value) return e;
		}
		return null;
	}
}