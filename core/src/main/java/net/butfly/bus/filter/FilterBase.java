package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.Keys;
import net.butfly.bus.Bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FilterBase implements Filter {
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	protected String id;
	protected Bus bus;
	protected FilterChain chain;

	public FilterBase() {
		this.id = Keys.key(String.class);
	}

	@Override
	public void initialize(Map<String, String> params) {}

	@Override
	public void execute(final FilterContext context) throws Exception {
		chain.executeNext(this, context);
	}
}