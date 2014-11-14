package net.butfly.bus.util.async;

import java.util.Map;
import java.util.concurrent.Callable;

import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.context.Context;

@Deprecated
public abstract class AsyncTask implements Callable<AsyncResult> {
	protected final Request request;
	protected final Map<String, Object> context;

	public AsyncTask(Request request, Map<String, Object> context) {
		super();
		request.context(Context.serialize(Context.toMap()));
		this.request = request;
		this.context = context;
	}

	@Override
	public AsyncResult call() throws Exception {
		Context.initialize(this.context, true);
		return new AsyncResult(this.doCall(), Context.toMap());
	}

	protected abstract Response doCall() throws Exception;
}
