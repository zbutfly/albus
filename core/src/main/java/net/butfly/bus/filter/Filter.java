package net.butfly.bus.filter;

import java.util.Map;

public interface Filter {
	void initialize(Map<String, String> params);

	void execute(FilterContext context) throws Exception;
}
