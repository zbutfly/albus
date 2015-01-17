package net.butfly.bus.impl;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;

public class RequestWrapper<R> {
	private Request request;
	private Options[] options;
	private Task.Callback<R> callback;

	public RequestWrapper(Request request, Options... options) {
		this(request, null, options);
	}

	public RequestWrapper(Request request, Task.Callback<R> callback, Options... options) {
		this.request = request;
		this.callback = callback;
		this.options = options;
	}

	public Request request() {
		return this.request;
	}

	public Options[] options() {
		return this.options;
	}

	public Task.Callback<R> callback() {
		return this.callback;
	}
}
