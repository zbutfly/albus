package net.butfly.bus.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.FilterBean;
import net.butfly.bus.filter.FilterBase.FilterRequest;
import net.butfly.bus.utils.Constants;

public final class FilterChain {
	private Filter[] filters;
	protected Map<String, Map<String, Object[]>> context = new HashMap<String, Map<String, Object[]>>();

	public FilterChain(Filter routeFilter, List<FilterBean> beans, Filter invokeFilter, Bus bus) {
		List<Filter> filters = new ArrayList<Filter>();
		this.addFilter(filters, routeFilter, bus);

		if (beans == null) beans = new ArrayList<FilterBean>();
		for (FilterBean bean : beans) {
			bean.getFilter().initialize(bean.getParams());
			this.addFilter(filters, bean.getFilter(), bus);
		}
		invokeFilter.initialize(new HashMap<String, String>());

		this.addFilter(filters, invokeFilter, bus);
		this.filters = filters.toArray(new Filter[filters.size()]);
	}

	private void addFilter(List<Filter> filters, Filter filter, Bus bus) {
		((FilterBase) filter).chain = this;
		((FilterBase) filter).bus = bus;
		this.context.put(((FilterBase) filter).id, ((FilterBase) filter).context);
		filters.add(filter);
	}

	public Object[] context(Filter filter, Request request) {
		return context.get(((FilterBase) filter).id).get(request.id());
	}

	public <R> Response execute(final Request request, Task.Callback<R> callback, final Options... options) throws Exception {
		FilterRequest<R> req = new FilterRequest<R>(request, callback, options);
		try {
			return this.executeOne(this.filters[0], req);
		} finally {
			for (Filter f : this.filters)
				((FilterBase) f).removeParams(req);
		}
	}

	private Response executeOne(Filter filter, FilterRequest<?> request) throws Exception {
		if (null == request.callback()) {
			filter.before(request);
			Response response = null;
			try {
				response = filter.execute(request);
			} finally {
				filter.after(request, response);
			}
			return response;
		} else {
			return new Task<Response>(new Task.Callable<Response>() {
				@Override
				public Response call() throws Exception {
					filter.before(request);
					return filter.execute(request);
				}
			}, new Task.Callback<Response>() {
				@Override
				public void callback(Response response) throws Exception {
					filter.after(request, response);
				}
			}).exception(new Task.ExceptionHandler<Response>() {
				@Override
				public Response handle(Exception ex) throws Exception {
					filter.after(request, null);
					throw ex;
				}
			}).execute();
		}

	}

	protected Response executeNext(Filter current, FilterRequest<?> request) throws Exception {
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
			return null;
		return this.executeOne(this.filters[pos + 1], request);
	}
}
