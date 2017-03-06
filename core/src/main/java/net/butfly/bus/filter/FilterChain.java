package net.butfly.bus.filter;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.async.Task;
import net.butfly.albacore.utils.async.Task.Callable;
import net.butfly.bus.Bus;
import net.butfly.bus.Bus.Mode;
import net.butfly.bus.Error;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.FilterConfig;
import net.butfly.bus.context.Context;
import net.butfly.bus.utils.Constants;

public final class FilterChain {
	private Filter[] filters;

	public FilterChain(Bus bus, FilterConfig... bean) {
		filters = new Filter[bean == null ? 2 : bean.length + 2];
		int i = 0;
		filters[i++] = process(new ContextFilter(), bus);
		if (bean != null) for (FilterConfig b : bean) {
			b.getFilter().initialize(b.getParams());
			filters[i++] = process(b.getFilter(), bus);
		}
		filters[i++] = process(new InvokerFilter(), bus);
	}

	private Filter process(Filter filter, Bus bus) {
		((FilterBase) filter).chain = this;
		((FilterBase) filter).bus = bus;
		return filter;
	}

	public Response execute(final FilterContext context) throws Exception {
		return new Task<Response>(new Callable<Response>() {
			@Override
			public Response call() {
				try {
					executeOne(filters[0], context);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return context.response();
			}
		}, context.callback(), context.invoker().localOptions(context.options())).execute();
	}

	private void executeOne(final Filter filter, final FilterContext context) throws Exception {
		try {
			filter.execute(context);
		} catch (Exception ex) {
			Throwable exx = Exceptions.unwrap(ex);
			if (context.mode() != Mode.SERVER) throw Exceptions.wrap(exx);
			context.response(new Response(context.request()).error(new Error(exx, Context.debug())));
		}
	}

	protected void executeNext(Filter current, FilterContext context) throws Exception {
		// TODO:optimizing...
		int pos = -1;
		for (int i = 0; i < this.filters.length; i++)
			if (this.filters[i].equals(current)) {
				pos = i;
				break;
			}
		if (pos == -1) // not found
			throw new SystemException(Constants.BusinessError.INVOKE_ERROR, "Filter not found.");
		if (pos == this.filters.length - 1) // last
			throw new SystemException(Constants.BusinessError.INVOKE_ERROR, "LastFilter should not run executeNext.");;
		executeOne(filters[pos + 1], context);
	}

	/**
	 * Kernal invoking of this bus.
	 */
	private static final class InvokerFilter extends FilterBase implements Filter {
		@Override
		public void execute(FilterContext context) throws Exception {
			context.response(context.invoker().invoke(context.request(), context.invoker().remoteOptions(context.options())));
		}
	};

	/**
	 * <ul>
	 * <li>1. Context <==> request/response</li>
	 * <li>2. Repsonse error ==> Exception</li>
	 * </ul>
	 */
	private static final class ContextFilter extends FilterBase implements Filter {
		@Override
		public void execute(final FilterContext context) throws Exception {
			switch (context.mode()) {
			case CLIENT:
				context.request().context(Context.serialize(Context.toMap()));
				try {
					super.execute(context);
				} finally {
					if (context.response() != null) {
						Context.merge(Context.deserialize(context.response().context()));
						if (null != context.response().error()) throw context.response().error().toException();
					}
				}
				break;
			case SERVER:
				Context.merge(Context.deserialize(context.request().context()));
				try {
					super.execute(context);
				} finally {
					context.response().context(Context.serialize(Context.toMap()));
				}
				break;
			}
		}
	}
}
