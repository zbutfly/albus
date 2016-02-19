package net.butfly.bus.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.butfly.albacore.exception.SystemException;

public class JSONSerializer extends SerializerBase implements Serializer {
	private static ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
			.enable(SerializationFeature.WRITE_ENUMS_USING_INDEX)
			.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
			.enable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS).enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	public JSONSerializer(Charset charset) {
		super(charset);
	}

	@Override
	public byte[] serialize(Object obj) {
		return asString(obj).getBytes(this.charset());
	}

	@Override
	public <T> T deserialize(byte[] data, Type type) {
		return fromString(new String(data, this.charset()), type);
	}

	@Override
	public Object[] deserialize(byte[] data, Type[] types) {
		return fromString(new String(data, this.charset()), types);
	}

	@Override
	public void write(OutputStream os, Object obj) throws IOException {
		os.write(this.serialize(obj));
		os.flush();
	}

	@Override
	public <T> T read(InputStream is, Type type) throws IOException {
		return this.deserialize(IOUtils.toByteArray(is), type);
	}

	@Override
	public Object[] read(InputStream is, Type[] types) throws IOException {
		return this.deserialize(IOUtils.toByteArray(is), types);
	}

	@Override
	public void readThenWrite(InputStream is, OutputStream os, Type... types) throws IOException {
		write(os, read(is, types));
	}

	private static final String[] SUPORT_MIME = new String[] { ContentType.APPLICATION_JSON.getMimeType() };

	@Override
	public String[] supportedMimeTypes() {
		return SUPORT_MIME;
	}

	@Override
	public boolean supportClass() {
		return false;
	}

	@Override
	public String asString(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new SystemException("", e);
		}
	}

	@Override
	public Object[] fromString(String str, Type[] types) {
		try {
			final JsonNode node = mapper.readTree(str);
			if (node.isNull()) return null;
			// 1 argument required, dynamic array wrapping.
			if (types.length == 1) {
				Object r;
				// array wrapped parameters
				if (node.isArray()) r = node.size() == 0 ? null : mapper.treeToValue(node.get(0), (Class<?>) types[0]);
				// 1 parameter only
				else r = mapper.treeToValue(node, (Class<?>) types[0]);
				return new Object[] { r };
			} else {
				if (!node.isArray()) throw new IllegalArgumentException("Need array for multiple arguments.");
				Iterator<JsonNode> it = node.iterator();
				Object[] args = new Object[types.length];
				for (int i = 0; i < types.length; i++) {
					if (!it.hasNext()) throw new IllegalArgumentException("Less amount arguments recieved than required.");
					args[i] = mapper.treeToValue(it.next(), (Class<?>) types[i]);
				}
				return args;
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid JSON.");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T fromString(String str, Type type) {
		try {
			return mapper.readValue(str, (Class<T>) type);
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid JSON.");
		}
	}
}
