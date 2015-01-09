package net.butfly.bus.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.utils.Constants.Side;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FilterBase implements Filter {
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	FilterChain chain;
	protected Side side;
	private Map<String, Object[]> context = new ConcurrentHashMap<String, Object[]>();

	@Override
	public void initialize(Map<String, String> params, Side side) {
		this.side = side;
	}

	@Override
	public Response execute(Request request) throws Exception {
		return chain.executeNext(this, request);
	}

	@Override
	public void before(Request request) {}

	@Override
	public void after(Request request, Response response) {}

	protected final void putParams(Request request, Object... params) {
		context.put(request.id(), params);

	}

	protected final Object[] getParams(Request request) {
		return context.get(request.id());
	}

	protected final void removeParams(Request request) {
		this.context.remove(request.id());
	}
}