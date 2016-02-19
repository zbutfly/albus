package net.butfly.bus.context;

import java.util.Map;

public abstract class WrappedContext extends Context {
	protected abstract void current(SimpleContext c);

	@Override
	protected void load(Map<String, Object> original) {
		Context c = current();
		if (null == c) {
			c = new SimpleContext();
			this.current((SimpleContext) c);
		}
		super.load(original);
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
