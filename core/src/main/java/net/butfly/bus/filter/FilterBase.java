package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.KeyUtils;
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
		try {
			before(context);
			chain.executeNext(this, context);
			after(context);
		} catch (Exception ex) {
			exception(context, ex);
		}
	}

	@Override
	public void before(FilterContext context) throws Exception {}

	@Override
	public void after(FilterContext context) throws Exception {}

	@Override
	public Response exception(FilterContext context, Exception exception) throws Exception {
		throw exception;
	}
}