package net.butfly.bus.argument;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Response implements Serializable {
	private static final long serialVersionUID = 5897857767191140750L;

	protected String id;
	protected String requestId;

	private Object result = null;
	private Map<String, String> context = new HashMap<String, String>();
	private Error error = null;

	public Response(Request request) {
		this.id = UUID.randomUUID().toString();
		this.requestId = request.id;
	}

	public Object result() {
		return result;
	}

	public Response result(Object result) {
		this.result = result;
		return this;
	}

	public Response context(Map<String, String> context) {
		this.context.putAll(context);
		return this;
	}

	public Map<String, String> context() {
		return this.context;
	}

	public String context(String key) {
		return this.context.get(key);
	}

	public String context(String key, String value) {
		String val = this.context.get(key);
		this.context.put(key, value);
		return val;
	}

	public Error error() {
		return error;
	}

	public Response error(Error error) {
		this.error = error;
		return this;
	}

	public String id() {
		return id;
	}
}
