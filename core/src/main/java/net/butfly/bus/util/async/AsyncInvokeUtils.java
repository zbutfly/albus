package net.butfly.bus.util.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.butfly.albacore.utils.AsyncTask;
import net.butfly.albacore.utils.UtilsBase;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;

public class AsyncInvokeUtils extends UtilsBase {
	private static class InvokeResult {
		Response response;
		Context context;
	}

	public static interface InvokeTaskCallback {
		Response invoke();
	}

	public static class InvokeTask extends AsyncTask<InvokeResult> {
		private Context context;
		private InvokeTaskCallback invokeCallback;

		public InvokeTask(InvokeTaskCallback invokeCallback, final AsyncCallback<Response> responseCallback, Context context) {
			super(new AsyncCallback<InvokeResult>() {
				@Override
				public void callback(InvokeResult result) {
					Context.merge(result.context);
					responseCallback.callback(result.response);
				}
			});
			this.invokeCallback = invokeCallback;
			this.context = context;
		}

		@Override
		public InvokeResult call() throws Exception {
			Context.folk(context);
			InvokeResult r = new InvokeResult();
			this.context.putAll(Context.CURRENT);
			r.response = this.invokeCallback.invoke();
			r.context = Context.CURRENT;
			return r;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(AsyncInvokeUtils.class);

	public static void handleBySignal(HandledBySignal handled) throws Throwable {
		boolean retry = true;
		while (retry)
			try {
				handled.handle();
				// TODO: renew retry count?
			} catch (Signal.Suspend signal) {
				logger.debug("Request is asked to be suspend for [" + signal.timeout() + "]");
				if (signal.timeout() > 0) try {
					Thread.sleep(signal.timeout());
				} catch (InterruptedException e) {}
			} catch (Signal.Timeout signal) {
				logger.debug("Request timeout, attemp to retry: [" + handled.request.retried() + "], do retry again? [" + retry
						+ "].");
				retry = handled.retry();
			} catch (Signal.Completed signal) {
				retry = false;
			}

	}
}
