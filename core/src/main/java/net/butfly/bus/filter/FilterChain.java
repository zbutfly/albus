package net.butfly.bus.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.config.bean.FilterBean;

public final class FilterChain {
	private Filter[] filters;

	public FilterChain(List<FilterBean> beans, Filter main, Side side) {
		List<Filter> filters = new ArrayList<Filter>();
		if (beans == null) beans = new ArrayList<FilterBean>();
		for (FilterBean bean : beans) {
			bean.getFilter().initialize(bean.getParams(), side);
			this.addFilter(filters, bean.getFilter());
		}
		main.initialize(new HashMap<String, String>(), side);
		this.addFilter(filters, main);
		this.filters = filters.toArray(new Filter[filters.size()]);
	}

	private void addFilter(List<Filter> list, Filter filter) {
		((FilterBase) filter).chain = this;
		list.add(filter);
	}

	public Response execute(Request request) throws Exception {
		Response r;
		try {
			r = this.executeOne(this.filters[0], request);
		} finally {
			for (Filter f : this.filters)
				((FilterBase) f).removeParams(request);
		}
		return r;
	}

	public void executeAfter(Request request, Response response) {
		for (Filter f : this.filters)
			f.after(request, response);
	}

	private Response executeOne(Filter filter, Request request) throws Exception {
		filter.before(request);
		Response response = null;;
		try {
			response = filter.execute(request);
		} finally {
			filter.after(request, response);
		}
		return response;
	}

	protected Response executeNext(Filter current, Request request) throws Exception {
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
