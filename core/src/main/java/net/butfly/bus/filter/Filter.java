package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.argument.Constants.Side;

public interface Filter {
	public void initialize(Map<String, String> params, Side side);

	public Response execute(Request request) throws Signal;

	public void before(Request request);

	public void after(Request request, Response response);
}
