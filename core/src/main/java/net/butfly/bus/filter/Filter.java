package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.bus.Response;
import net.butfly.bus.filter.FilterBase.FilterRequest;

public interface Filter {
	void initialize(Map<String, String> params);

	<R> Response execute(FilterRequest<R> request) throws Exception;

	void before(FilterRequest<?> request);

	void after(FilterRequest<?> request, Response response) throws Exception;
}
