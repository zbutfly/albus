package net.butfly.bus.filter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.butfly.albacore.utils.async.Callable;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.utils.async.AsyncUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author butfly
 * @deprecated insteaded by {@link net.butfly.bus.filter.AsyncFilter} to enable
 *             work-stealing mode (with callback pattern)
 */
@Deprecated
public class ThreadControlFilter extends FilterBase implements Filter {
	private static Logger logger = LoggerFactory.getLogger(ThreadControlFilter.class);
	private ExecutorService executor;
	private long timeout;

	@Override
	public void initialize(Map<String, String> params, Side side) {
		super.initialize(params, side);
		if (this.side == Side.CLIENT)
			logger.warn("Use AsyncBus for client async instead of ThreadControlFilter to abtain more performance and control.");
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
	public Response execute(Request request) throws Signal {
		return AsyncUtils.execute(new Task<Response>(new Callable<Response>() {
			@Override
			public Response call() throws Signal {
				return ThreadControlFilter.super.execute(request);
			}
		}, new Options().timeout(timeout)), executor);
	}
}
