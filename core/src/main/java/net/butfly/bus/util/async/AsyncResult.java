package net.butfly.bus.util.async;

import java.io.Serializable;

import net.butfly.bus.Response;
import net.butfly.bus.context.Context;

@Deprecated
public final class AsyncResult implements Serializable {
	private static final long serialVersionUID = -4929636298814911427L;
	private Response response;
	private Context context;

	public AsyncResult(Response response, Context context) {
		super();
		this.response = response;
		this.context = context;
	}

	public Response getResponse() {
		return response;
	}

	public Context getContext() {
		return context;
	}
}
