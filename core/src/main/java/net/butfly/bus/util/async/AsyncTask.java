package net.butfly.bus.util.async;

import java.util.concurrent.Callable;

import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;

@Deprecated
public abstract class AsyncTask implements Callable<AsyncResult> {
	protected final Request request;
	protected final Context context;

	public AsyncTask(Request request, Context context) {
		super();
		request.context(Context.serialize(Context.toMap()));
		this.request = request;
		this.context = context;
	}

	@Override
	public AsyncResult call() throws Exception {
		Context.folk(context);
		return new AsyncResult(this.doCall(), Context.CURRENT);
	}

	protected abstract Response doCall() throws Exception;
}
