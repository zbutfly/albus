package net.butfly.bus.util.async;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.AsyncTask;
import net.butfly.albacore.utils.AsyncUtils;
import net.butfly.albacore.utils.UtilsBase;
import net.butfly.bus.argument.Response;
import net.butfly.bus.context.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncInvokeUtils extends UtilsBase {
	private static class InvokeResult {
		Response response;
		Context context;
	}

	public static interface InvokeTaskCallback {
		Response invoke();
	}

	public static interface RepeatInvokeTaskCallback {
		ObjectInputStream invoke();
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

	private static class ProducerTask extends AsyncTask<Object> {
		private ObjectOutputStream os;
		private int retries;
		private AsyncTask<?> task;
		private Context context;

		public ProducerTask(ObjectOutputStream os, AsyncTask<?> task, Context context, int retries) {
			super(new AsyncCallback<Object>() {
				@Override
				public void callback(Object nill) {
					logger.info("Invoking repeated and finished.");
				}
			});
			this.os = os;
			this.retries = retries;
			this.task = task;
			this.context = context;
		}

		@Override
		public Object call() throws Exception {
			Context.folk(context);
			produce(os, task, retries);
			this.context.putAll(Context.CURRENT);
			return null;
		}
	}

	private static void produce(ObjectOutputStream os, AsyncTask<?> task, int retries) throws Exception {
		if (retries < 0) while (true) {
			os.writeObject(task.call());
			os.flush();
		}
		else for (int i = 0; i < retries; i++) {
			os.writeObject(task.call());
			os.flush();
		}
		os.close();
	}

	private static class ConsumerTask extends AsyncTask<Object> {
		private ObjectInputStream is;
		private AsyncTask<Response> task;
		private Context context;

		public ConsumerTask(ObjectInputStream is, AsyncTask<Response> task, Context context) {
			super(new AsyncCallback<Object>() {
				@Override
				public void callback(Object nill) {
					logger.info("Invoking repeated and producer finished.");
				}
			});
			this.is = is;
			this.task = task;
			this.context = context;
		}

		@Override
		public Object call() throws Exception {
			Context.folk(context);
			try {
				consume(is, task);
			} finally {
				this.context.putAll(Context.CURRENT);
			}
			return null;
		}
	}

	private static void consume(ObjectInputStream is, AsyncTask<Response> task) throws IOException {
		try {
			while (true)
				try {
					task.getCallback().callback((Response) is.readObject());
				} catch (OptionalDataException ex) {
					if (ex.eof) {
						logger.info("Invoking repeated and consumer finished.");
						return;
					} else {
						throw new SystemException("", ex);
					}
				} catch (Throwable th) {
					throw new SystemException("", th);
				}
		} finally {
			is.close();
		}
	}

	public static class InvokeOption {
		private boolean fork;
		private long timeout;

		public InvokeOption() {
			super();
			this.fork = false;
			this.timeout = -1;
		}

		public InvokeOption(boolean fork) {
			super();
			this.fork = fork;
			this.timeout = -1;
		}

		public InvokeOption(long timeout) {
			super();
			this.fork = true;
			this.timeout = timeout;
		}
	}

	public static void invoke(final AsyncTask<Response> task, final InvokeOption producerOpt, final InvokeOption consumerOpt,
			final int retries) {
		PipedOutputStream pout = new PipedOutputStream();
		ObjectOutputStream os = null;
		ObjectInputStream is = null;
		try {
			os = new ObjectOutputStream(pout);
			is = new ObjectInputStream(new PipedInputStream(pout));
			ProducerTask producer = new ProducerTask(os, task, Context.CURRENT, retries);
			ConsumerTask consumer = new ConsumerTask(is, task, Context.CURRENT);
			if (producerOpt != null && producerOpt.fork) AsyncUtils.invoke(producer, producerOpt.timeout);
			if (consumerOpt != null && consumerOpt.fork) AsyncUtils.invoke(consumer, consumerOpt.timeout);
			if (producerOpt == null || !producerOpt.fork) produce(os, task, retries);
			if (consumerOpt == null || !consumerOpt.fork) consume(is, task);
		} catch (Exception e) {
			throw new SystemException("");
		} finally {
			// if (null != is) try {
			// is.close();
			// } catch (IOException e) {
			// throw new SystemException("");
			// }
			// if (null != os) try {
			// os.close();
			// } catch (IOException e) {
			// throw new SystemException("");
			// }
		}
	}
}
