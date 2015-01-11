package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Response;
import net.butfly.bus.utils.BusTask;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.RequestWrapper;

public class AsyncFilter extends FilterBase implements Filter {
	private long timeout;

	@Override
	public void initialize(Map<String, String> params) {
		super.initialize(params);
		String val = params.get("timeout");
		this.timeout = null == val ? Constants.Async.DEFAULT_TIMEOUT : Long.parseLong(val);
	}

	@Override
	public Response execute(final RequestWrapper<?> request) throws Exception {
		return new BusTask<Response>(new Task.Callable<Response>() {
			@Override
			public Response call() throws Exception {
				return AsyncFilter.super.execute(request);
			}
		}, new Options().timeout(timeout)).execute();
	}
}
