package net.butfly.bus.filter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Response;
import net.butfly.bus.utils.BusTask;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.RequestWrapper;

/**
 * @author butfly
 * @deprecated insteaded by {@link net.butfly.bus.filter.AsyncFilter} to enable work-stealing mode (with
 *             callback pattern)
 */
@Deprecated
public class ThreadControlFilter extends FilterBase implements Filter {
	private ExecutorService executor;
	private long timeout;

	@Override
	public void initialize(Map<String, String> params) {
		super.initialize(params);
		String val = params.get("corePoolSize");
		int corePoolSize = null == val ? Constants.Async.DEFAULT_CORE_POOL_SIZE : Integer.parseInt(val);
		val = params.get("maxPoolSize");
		int maxPoolSize = null == val ? Constants.Async.DEFAULT_MAX_POOL_SIZE : Integer.parseInt(val);
		val = params.get("keepAliveTime");
		long keepAliveTime = null == val ? Constants.Async.DEFAULT_ALIVE_TIME : Long.parseLong(val);
		this.executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS,
				new SynchronousQueue<Runnable>());
		val = params.get("timeout");
		this.timeout = null == val ? Constants.Async.DEFAULT_TIMEOUT : Long.parseLong(val);
	}

	@Override
	public Response execute(final RequestWrapper<?> request) throws Exception {
		return new BusTask<Response>(new Task.Callable<Response>() {
			@Override
			public Response call() throws Exception {
				return ThreadControlFilter.super.execute(request);
			}
		}, new Options().timeout(timeout)).execute(executor);
	}
}
