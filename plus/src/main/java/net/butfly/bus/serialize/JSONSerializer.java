package net.butfly.bus.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.http.entity.ContentType;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class JSONSerializer extends HTTPStreamingSupport implements Serializer {
	private Gson gson = new Gson();
	private JsonParser parser = new JsonParser();

	@Override
	public void write(OutputStream os, Object obj) throws IOException {
		os.write(gson.toJson(obj).getBytes(this.getOutputContentType().getCharset()));
		os.flush();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T read(InputStream is, Class<?>... types) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(is, this.getOutputContentType().getCharset()));
		try {
			JsonElement ele = parser.parse(reader);
			if (ele.isJsonNull()) return null;
			if (ele.isJsonObject() || ele.isJsonPrimitive()) {
				if (types.length < 1) throw new IllegalArgumentException();
				return (T) gson.fromJson(ele, types[0]);
			}
			if (ele.isJsonArray()) {
				JsonArray arr = ele.getAsJsonArray();
				int len = Math.min(arr.size(), types.length);
				Object[] args = new Object[len];
				for (int i = 0; i < len; i++)
					args[i] = gson.fromJson(arr.get(i), types[i]);
				return (T) args;
			}
			throw new IllegalArgumentException();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void readThenWrite(InputStream is, OutputStream os, Class<?>... types) throws IOException {
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
}
