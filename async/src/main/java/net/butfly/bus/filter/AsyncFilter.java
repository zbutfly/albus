package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.utils.BusTask;
import net.butfly.bus.utils.Constants;

public class AsyncFilter extends FilterBase implements Filter {
	private long timeout;

	@Override
	public void initialize(Map<String, String> params) {
		super.initialize(params);
		String val = params.get("timeout");
		this.timeout = null == val ? Constants.Async.DEFAULT_TIMEOUT : Long.parseLong(val);
	}

	@Override
	public void execute(final FilterContext context) throws Exception {
		new BusTask<Void>(new Task<Void>(new Task.Callable<Void>() {
			@Override
			public Void call() throws Exception {
				AsyncFilter.super.execute(context);
				return null;
			}
		}, new Options().timeout(timeout))).execute();
	}
}
