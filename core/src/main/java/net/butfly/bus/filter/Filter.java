package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.bus.Response;
import net.butfly.bus.impl.RequestWrapper;

public interface Filter {
	public void initialize(Map<String, String> params);

	public Response execute(RequestWrapper<?> request) throws Exception;

	public void before(RequestWrapper<?> request);

	public void after(RequestWrapper<?> request, Response response);
}
