package net.butfly.bus.utils.async;

import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.async.Callable;
import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;

/**
 * @author butfly
 *
 *         Wrapper class for {@link net.butfly.albacore.utils.async.Task
 *         <Response>}, support {@link net.butfly.bus.context.Context}
 *         transporting between async threads.
 */
public class InvokeTask extends Task<Response> {
	private final Map<String, Object> context = new HashMap<String, Object>();

	public InvokeTask(final Task<Response> original) {
		context.putAll(Context.toMap());
		this.task = new Callable<Response>() {
			@Override
			public Response call() throws Signal {
				Context.initialize(context, true);
				Response r = original.task().call();
				context.putAll(Context.toMap());
				return r;
			}
		};
		this.callback = new Callback<Response>() {
			@Override
			public void callback(Response result) throws Signal {
				Context.initialize(context, true);
				original.callback().callback(result);
				context.putAll(Context.toMap());
			}
		};
		this.options = original.options();
	}
}
