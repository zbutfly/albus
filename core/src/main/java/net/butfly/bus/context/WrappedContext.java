package net.butfly.bus.context;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WrappedContext extends Context {
	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	protected abstract void current(SimpleContext c);

	@Override
	protected void load(Map<String, Object> original) {
		Context c = current();
		if (null == c) {
			c = new SimpleContext();
			this.current((SimpleContext) c);
		}
		super.load(original);;
	}

	@Override
	protected Map<String, Object> impl() {
		return current().impl();
	}

	@Override
	protected boolean sharing() {
		return true;
	}
}
