package net.butfly.bus.filter;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.context.Context;
import net.butfly.bus.util.async.AsyncResult;
import net.butfly.bus.util.async.AsyncTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: upgrade to callback pattern and work-stealing mode -- use AsyncFilter
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
	public Response execute(Request request) throws Exception {
		FutureTask<AsyncResult> task = new FutureTask<AsyncResult>(new AsyncTask(request, Context.CURRENT) {
			@Override
			protected Response doCall() throws Exception {
				return ThreadControlFilter.super.execute(request);
			}
		});
		try {
			executor.execute(task);
		} catch (RejectedExecutionException e) {
			logger.warn("async task executing rejected for pool saturated...");
			throw new SystemException(Constants.SystemError.SATURATED, "Request pool overflow.");
		}
		try {
			AsyncResult r = null;
			if (timeout > 0) r = task.get(timeout, TimeUnit.MILLISECONDS);
			else r = task.get();
			logger.trace("Request completed.");
			return r.getResponse();
		} catch (InterruptedException e) {
			task.cancel(true);
			throw new SystemException(Constants.SystemError.INTERRUPTED, e);
		} catch (TimeoutException e) {
			task.cancel(true);
			throw new SystemException(Constants.SystemError.TIMEOUT, "Request timeout.");
		} catch (ExecutionException e) {
			task.cancel(true);

			Throwable ex = e.getCause();
			if (ex instanceof Exception) throw (Exception) ex;
			logger.error("Unhandlable exception: ", ex);
			throw new SystemException(Constants.SystemError.UNKNOW_CAUSE, ex);
		}
	}
}
