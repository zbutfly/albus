package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.bus.Response;

public interface Filter {
	void initialize(Map<String, String> params);

	void before(FilterContext context) throws Exception;

	<R> void execute(FilterContext context) throws Exception;

	void after(FilterContext context) throws Exception;

	Response exception(FilterContext context, Exception exception) throws Exception;
}
