package net.butfly.bus.argument;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;

public class AsyncRequest extends Request {
	private static final long serialVersionUID = -5649661894998681227L;
	private final AsyncCallback<Response> callback;
	private final int timeout;
	private final int retries;
	private int retried = 0;

	public AsyncRequest(Request request, AsyncCallback<Response> callback) {
		this(request, callback, -1);
	}

	public AsyncRequest(Request request, AsyncCallback<Response> callback, int retries) {
		this(request, callback, 0, retries);
	}

	public AsyncRequest(Request request, AsyncCallback<Response> callback, int timeout, int retries) {
		super(request.code(), request.version(), request.context(), request.arguments());
		this.callback = callback;
		this.timeout = timeout;
		this.retries = retries;
	}

	public AsyncCallback<Response> callback() {
		return this.callback;
	}

	public int timeout() {
		return timeout;
	}

	public boolean retry() {
		return this.retried++ <= this.retries;
	}

	public int retried() {
		return this.retried;
	}

	public boolean continuous() {
		return this.retries >= 0;
	}

	public int retries() {
		return this.retries;
	}
}
