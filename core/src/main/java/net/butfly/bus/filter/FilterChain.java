package net.butfly.bus.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.FilterBean;
import net.butfly.bus.utils.Constants;

public final class FilterChain {
	private Filter[] filters;

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
		filters.add(filter);
	}

	public Response execute(final Request request, Task.Callback<Response> callback, final Options... options) throws Exception {
		FilterContext context = new FilterContext(request, callback, options);
		executeOne(filters[0], context);
		return context.response();
	}

	private void executeOne(final Filter filter, final FilterContext context) throws Exception {
		filter.execute(context);
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
}
