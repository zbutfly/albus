package net.butfly.bus.filter;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus.Mode;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.invoker.Invoker;

public class FilterContext {
	private Mode mode;
	private Request request;
	private Response response;
	private Options[] options;
	private Task.Callback<Response> callback;
	private Invoker<?> invoker;

	public FilterContext(Invoker<?> invoker, Request request, Task.Callback<Response> callback, Mode mode, Options... options) {
		this.invoker = invoker;
		this.request = request;
		this.callback = callback;
		this.options = options;
		this.mode = mode;
	}

	public Request request() {
		return this.request;
	}

	public Options[] options() {
		return this.options;
	}

	public Task.Callback<Response> callback() {
		return this.callback;
	}

	public Invoker<?> invoker() {
		return this.invoker;
	}

	public Response response() {
		return response;
	}

	public FilterContext response(Response response) {
		this.response = response;
		return this;
	}

	public Mode mode() {
		return this.mode;
	}
}
