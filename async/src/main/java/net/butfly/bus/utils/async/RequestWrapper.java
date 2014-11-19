package net.butfly.bus.utils.async;

import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;

public class RequestWrapper extends Request {
	private static final long serialVersionUID = 7284007663713259222L;
	private Options options;

	public RequestWrapper(Request request, Options options) {
		super(request.id(), request.code(), request.version(), request.context(), request.arguments());
		this.options = options;
	}

	public Options options() {
		return this.options;
	}
}
