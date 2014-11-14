package net.butfly.bus.util.async;

import java.io.Serializable;
import java.util.Map;

import net.butfly.bus.argument.Response;

@Deprecated
public final class AsyncResult implements Serializable {
	private static final long serialVersionUID = -4929636298814911427L;
	private Response response;
	private Map<String, Object> context;

	public AsyncResult(Response response, Map<String, Object> context) {
		super();
		this.response = response;
		this.context = context;
	}

	public Response getResponse() {
		return response;
	}

	public Map<String, Object> getContext() {
		return context;
	}
}
