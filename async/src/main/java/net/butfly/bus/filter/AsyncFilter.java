package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Constants.Side;

public class AsyncFilter extends FilterBase implements Filter {
	private long timeout;

	@Override
	public void initialize(Map<String, String> params, Side side) {
		super.initialize(params, side);
		String val = params.get("timeout");
		this.timeout = null == val ? Constants.Async.DEFAULT_TIMEOUT : Long.parseLong(val);
	}

	@Override
	public Response execute(final Request request) throws Exception {
		return new Task<Response>(new Task.Callable<Response>() {
			@Override
			public Response call() throws Exception {
				return AsyncFilter.super.execute(request);
			}
		}, new Options().timeout(timeout)).execute();
	}
}
