package net.butfly.bus.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class JSONSerializer extends HTTPStreamingSupport implements Serializer {
	private Gson gson = new Gson();// add TypeAdapterFactory here.

	@Override
	public byte[] serialize(Object obj) {
		return asString(obj).getBytes(this.getOutputContentType().getCharset());
	}

	@Override
	public <T> T deserialize(byte[] data, Type... types) {
		return fromString(new String(data, this.getOutputContentType().getCharset()), types);
	}

	@Override
	public void write(OutputStream os, Object obj) throws IOException {
		os.write(this.serialize(obj));
		os.flush();
	}

	@Override
	public <T> T read(InputStream is, Type... types) throws IOException {
		return this.deserialize(IOUtils.toByteArray(is), types);
	}

	@Override
	public void readThenWrite(InputStream is, OutputStream os, Type... types) throws IOException {
		write(os, read(is, types));
	}

	@Override
	public boolean supportHTTPStream() {
		return true;
	}

	@Override
	public ContentType[] getSupportedContentTypes() {
		return new ContentType[] { ContentType.APPLICATION_JSON };
	}

	@Override
	public boolean supportClass() {
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T> T parseJSON(JsonElement json, Type... types) {
		if (json.isJsonNull()) return null;
		if (types.length == 1) {
			Type t = types[0];
			if (json.isJsonArray()) {
				if (TypeToken.of(t).isArray()) return gson.fromJson(json, t);
				else {
					Object[] args = new Object[1];
					args[0] = gson.fromJson(json.getAsJsonArray().get(0), t);
					return (T) args;
				}
			}
			if (json.isJsonObject() || json.isJsonPrimitive()) return (T) gson.fromJson(json, t);
		}
		if (types.length > 1) {
			if (json.isJsonArray()) {
				JsonArray arr = json.getAsJsonArray();
				int len = Math.min(arr.size(), types.length);
				Object[] args = new Object[len];
				for (int i = 0; i < len; i++)
					args[i] = gson.fromJson(arr.get(i), types[i]);
				return (T) args;
			}
			if (json.isJsonObject() || json.isJsonPrimitive()) return (T) gson.fromJson(json, types[0]);
		}
		throw new IllegalArgumentException();
	}

	@Override
	public String asString(Object obj) {
		return gson.toJson(obj);
	}

	@Override
	public <T> T fromString(String str, Type... types) {
		return this.parseJSON(gson.fromJson(str, JsonElement.class), types);
	}
}
