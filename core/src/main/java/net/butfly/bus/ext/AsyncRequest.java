package net.butfly.bus.ext;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.Request;
import net.butfly.bus.Response;

public class AsyncRequest extends Request {
	private static final long serialVersionUID = -5649661894998681227L;
	private final AsyncCallback<Response> callback;
	private final long timeout;
	private final boolean continuous;
	private final int retries;
	private int retried = 0;

	public AsyncRequest(Request request, AsyncCallback<Response> callback, long timeout) {
		this(request, callback, timeout, 3);
	}

	public AsyncRequest(Request request, AsyncCallback<Response> callback, long timeout, int retries) {
		super(request.code(), request.version(), request.context(), request.arguments());
		this.callback = callback;
		this.timeout = timeout;
		this.continuous = false;
		this.retries = retries;
	}

	public AsyncRequest(Request request, AsyncCallback<Response> callback, boolean continuous) {
		this(request, callback, continuous, -1);
	}

	public AsyncRequest(Request request, AsyncCallback<Response> callback, boolean continuous, int retries) {
		super(request.id(), request.code(), request.version(), request.context(), request.arguments());
		this.callback = callback;
		this.timeout = 0;
		this.continuous = continuous;
		this.retries = retries;
	}

	public AsyncCallback<Response> callback() {
		return this.callback;
	}

	public long timeout() {
		return timeout;
	}

	public boolean continuous() {
		return this.continuous;
	}

	public Request request() {
		return new Request(this.code(), this.version(), this.context(), this.arguments());
	}

	public boolean retry() {
		return this.retried++ <= this.retries;
	}

	public int retried() {
		return this.retried;
	}
}
