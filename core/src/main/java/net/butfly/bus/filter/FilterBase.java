package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus;
import net.butfly.bus.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FilterBase implements Filter {
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	protected String id;
	protected Bus bus;
	protected FilterChain chain;

	public FilterBase() {
		this.id = KeyUtils.defaults();
	}

	@Override
	public void initialize(Map<String, String> params) {}

	@Override
	public void execute(final FilterContext context) throws Exception {
		if (null == context.callback()) {
			try {
				before(context);
				chain.executeNext(this, context);
				after(context);
			} catch (Exception ex) {
				exception(context, ex);
			}
		} else {
			new Task<Response>(new Task.Callable<Response>() {
				@Override
				public Response call() throws Exception {
					before(context);
					chain.executeNext(FilterBase.this, context);
					return context.response();
				}
			}, new Task.Callback<Response>() {
				@Override
				public void callback(Response response) {
					after(context);
				}
			}).handler(new Task.ExceptionHandler<Response>() {
				@Override
				public Response handle(Exception ex) throws Exception {
					return exception(context, ex);
				}
			}).execute();
		}
	}

	@Override
	public void before(FilterContext context) throws Exception {}

	@Override
	public void after(FilterContext context) {}

	@Override
	public Response exception(FilterContext context, Exception exception) throws Exception {
		throw exception;
	}
}