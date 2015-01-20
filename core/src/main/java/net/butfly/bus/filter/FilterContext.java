package net.butfly.bus.filter;

import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.invoker.Invoker;

public class FilterContext {
	private Request request;
	private Response response;
	private Options[] options;
	private Task.Callback<Response> callback;
	private Invoker<?> invoker;
	private Map<String, Object> params = new HashMap<String, Object>();

	public FilterContext(Request request, Options... options) {
		this(request, null, options);
	}

	public FilterContext(Request request, Task.Callback<Response> callback, Options... options) {
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

	public Task.Callback<Response> callback() {
		return this.callback;
	}

	public Invoker<?> invoker() {
		return this.invoker;
	}

	public Response response() {
		return response;
	}

	@SuppressWarnings("unchecked")
	public <T> T param(String name) {
		return (T) this.params.get(name);
	}

	public FilterContext invoker(Invoker<?> invoker) {
		this.invoker = invoker;
		return this;
	}

	public FilterContext response(Response response) {
		this.response = response;
		return this;
	}

	public FilterContext param(String name, Object value) {
		this.params.put(name, value);
		return this;
	}
}
