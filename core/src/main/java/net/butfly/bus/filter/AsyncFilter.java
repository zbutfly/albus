package net.butfly.bus.filter;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.context.Context;
import net.butfly.bus.util.async.AsyncResult;
import net.butfly.bus.util.async.AsyncTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Deprecated
public class AsyncFilter extends FilterBase implements Filter {
	private static Logger logger = LoggerFactory.getLogger(AsyncFilter.class);
	private ListeningExecutorService executor;
	private long timeout;

	@Override
	public void initialize(Map<String, String> params, Side side) {
		super.initialize(params, side);
		if (this.side == Side.CLIENT)
			logger.warn("Use AsyncBus for client async instead of ThreadControlFilter to abtain more performance and control.");
		this.executor = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool());
		String val = params.get("timeout");
		this.timeout = null == val ? Constants.Async.DEFAULT_TIMEOUT : Long.parseLong(val);
	}

	@Override
	public Response execute(Request request) throws Exception {
		Future<AsyncResult> f = this.executor.invokeAll(Arrays.asList(new AsyncTask(request, Context.CURRENT) {
			@Override
			protected Response doCall() throws Exception {
				return AsyncFilter.super.execute(request);
			}
		})).iterator().next();
		AsyncResult r;
		if (timeout > 0) r = f.get(timeout, TimeUnit.MILLISECONDS);
		else r = f.get();
		logger.trace("Async invoking finished: " + r + ".");
		return r.getResponse();
	}
}
