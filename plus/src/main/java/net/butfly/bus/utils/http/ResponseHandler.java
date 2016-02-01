package net.butfly.bus.utils.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.Error;
import net.butfly.bus.Response;
import net.butfly.bus.serialize.Serializer;

import com.google.common.net.HttpHeaders;

public class ResponseHandler {
	private Map<String, List<String>> headers;
	private byte[] data;
	private Serializer serializer;

	public ResponseHandler(Serializer serializer, Map<String, List<String>> headers, byte[] data) {
		super();
		this.serializer = serializer;
		this.headers = headers;
		this.data = data;
	}

	public Map<String, String> context() {
		int prefixLen = BusHeaders.HEADER_CONTEXT_PREFIX.length();
		Map<String, String> ctx = new HashMap<String, String>();
		if (headers != null && headers.size() != 0) {
			for (String name : headers.keySet())
				if (name != null && name.startsWith(BusHeaders.HEADER_CONTEXT_PREFIX))
					ctx.put(name.substring(prefixLen), header(name));
		}
		return ctx;
	}

	public String header(String name) {
		if (headers == null || headers.size() == 0) return null;
		List<String> values = headers.get(name);
		if (null == values || values.size() == 0) return null;
		// TODO
		return values.get(0);
	}

	public Response response() throws IOException {
		Response response = new ResponseWrapper(header(HttpHeaders.ETAG), header(BusHeaders.HEADER_REQUEST_ID));
		response.context(context());
		if (Boolean.parseBoolean(header(BusHeaders.HEADER_ERROR))) {
			Error detail = serializer.deserialize(data, Error.class);
			response.error(detail);
		} else {
			if (Boolean.parseBoolean(header(BusHeaders.HEADER_CLASS_SUPPORT))) response.result(serializer.deserialize(data));
			else {
				Class<?> clazz = Reflections.forClassName(header(BusHeaders.HEADER_CLASS));
				response.result(null != clazz ? serializer.deserialize(data, clazz) : serializer.deserialize(data));
			}
		}
		return ((ResponseWrapper) response).unwrap();
	}

	private static class ResponseWrapper extends Response {
		private static final long serialVersionUID = 37612994599685817L;

		public ResponseWrapper(String id, String requestId) {
			super();
			this.id = id;
			this.requestId = requestId;
		}

		public Response unwrap() {
			return this;
		}
	}
}