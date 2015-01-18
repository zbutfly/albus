package net.butfly.bus.filter;

import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.invoker.Invoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FilterBase implements Filter {
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	protected String id;
	protected Bus bus;
	protected FilterChain chain;
	protected Map<String, Object[]> context;

	public FilterBase() {
		this.id = KeyUtils.defaults();
		this.context = new HashMap<String, Object[]>();
	}

	@Override
	public void initialize(Map<String, String> params) {}

	@Override
	public <R> Response execute(FilterRequest<R> request) throws Exception {
		return chain.executeNext(this, request);
	}

	@Override
	public void before(FilterRequest<?> request) {}

	@Override
	public void after(FilterRequest<?> request, Response response) throws Exception {}

	protected final void putParams(FilterRequest<?> request, Object... params) {
		context.put(request.request().id(), params);
	}

	protected final Object[] getParams(FilterRequest<?> request) {
		return context.get(request.request().id());
	}

	protected final void removeParams(FilterRequest<?> request) {
		this.context.remove(request.request().id());
	}

	protected static class FilterRequest<R> {
		private Request request;
		private Options[] options;
		private Task.Callback<R> callback;
		private Invoker<?> invoker;

		public FilterRequest(Request request, Options... options) {
			this(request, null, options);
		}

		public FilterRequest(Request request, Task.Callback<R> callback, Options... options) {
			this.request = request;
			this.callback = callback;
			this.options = options;
		}

		public Request request() {
			return this.request;
		}

		public Invoker<?> invoker() {
			return this.invoker;
		}
		public void invoker(Invoker<?> invoker) {
			this.invoker = invoker;
		}

		public Options[] options() {
			return this.options;
		}

		public Task.Callback<R> callback() {
			return this.callback;
		}
	}
}