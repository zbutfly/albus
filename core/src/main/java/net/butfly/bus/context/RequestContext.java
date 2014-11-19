package net.butfly.bus.context;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.albacore.exception.SystemException;

class RequestContext extends WrappedContext {
	private static final ThreadLocal<String> KEY_LOCAL = new ThreadLocal<String>();
	private static final Map<String, SimpleContext> CTX_LOCAL = new ConcurrentHashMap<String, SimpleContext>();

	@Override
	protected void initialize(Map<String, Object> original) {
		if (null == original) throw new SystemException("", "Context of Request is not initialized properly.");
		else if (!original.containsKey(Key.RequestID.name()))
		// emulate request id for local testing.
			original.put(Key.RequestID.name(), UUID.randomUUID().toString());
		KEY_LOCAL.set((String) original.get(Key.RequestID.name()));
		super.initialize(original);
	}

	@Override
	protected void current(SimpleContext c) {
		CTX_LOCAL.put(KEY_LOCAL.get(), c);
	};

	@Override
	protected Context current() {
		return CTX_LOCAL.get(KEY_LOCAL.get());
	};

	@Override
	public void clear() {
		super.clear();
		CTX_LOCAL.remove(KEY_LOCAL.get());
		KEY_LOCAL.remove();
	}
}
