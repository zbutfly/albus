package net.butfly.bus.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.FilterBean;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.RequestWrapper;

public final class FilterChain {
	private Filter[] filters;

	public FilterChain(List<FilterBean> beans, Filter main) {
		List<Filter> filters = new ArrayList<Filter>();
		if (beans == null) beans = new ArrayList<FilterBean>();
		for (FilterBean bean : beans) {
			bean.getFilter().initialize(bean.getParams());
			this.addFilter(filters, bean.getFilter());
		}
		main.initialize(new HashMap<String, String>());
		this.addFilter(filters, main);
		this.filters = filters.toArray(new Filter[filters.size()]);
	}

	private void addFilter(List<Filter> list, Filter filter) {
		((FilterBase) filter).chain = this;
		list.add(filter);
	}

	public Response execute(RequestWrapper<?> request) throws Exception {
		try {
			return this.executeOne(this.filters[0], request);
		} finally {
			for (Filter f : this.filters)
				((FilterBase) f).removeParams(request);
		}
	}

	public void executeAfter(RequestWrapper<?> request, Response response) {
		for (Filter f : this.filters)
			f.after(request, response);
	}

	private Response executeOne(Filter filter, RequestWrapper<?> request) throws Exception {
		filter.before(request);
		Response response = null;;
		try {
			response = filter.execute(request);
		} finally {
			filter.after(request, response);
		}
		return response;
	}

	protected Response executeNext(Filter current, RequestWrapper<?> request) throws Exception {
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
