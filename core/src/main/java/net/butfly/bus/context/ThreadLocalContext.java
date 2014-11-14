package net.butfly.bus.context;

class ThreadLocalContext extends WrappedContext {
	private static final ThreadLocal<SimpleContext> CTX_LOCAL = new ThreadLocal<SimpleContext>();

	@Override
	protected void current(SimpleContext c) {
		CTX_LOCAL.set(c);
	};

	@Override
	protected Context current() {
		return CTX_LOCAL.get();
	};

	@Override
	public void clear() {
		super.clear();
		CTX_LOCAL.remove();
	}
}
