package net.butfly.bus.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.bus.Response;
import net.butfly.bus.utils.RequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FilterBase implements Filter {
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	FilterChain chain;
	private Map<String, Object[]> context = new ConcurrentHashMap<String, Object[]>();

	@Override
	public void initialize(Map<String, String> params) {}

	@Override
	public Response execute(RequestWrapper<?> request) throws Exception {
		return chain.executeNext(this, request);
	}

	@Override
	public void before(RequestWrapper<?> request) {}

	@Override
	public void after(RequestWrapper<?> request, Response response) {}

	protected final void putParams(RequestWrapper<?> request, Object... params) {
		context.put(request.request().id(), params);

	}

	protected final Object[] getParams(RequestWrapper<?> request) {
		return context.get(request.request().id());
	}

	protected final void removeParams(RequestWrapper<?> request) {
		this.context.remove(request.request().id());
	}
}