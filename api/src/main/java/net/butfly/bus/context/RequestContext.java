package net.butfly.bus.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.albacore.utils.KeyUtils;

class RequestContext extends WrappedContext {
	private static final ThreadLocal<String> KEY_LOCAL = new ThreadLocal<String>();
	private static final Map<String, SimpleContext> CTX_LOCAL = new ConcurrentHashMap<String, SimpleContext>();

	@Override
	protected void load(Map<String, Object> original) {
		if (null == original) original = new HashMap<String, Object>();
		// emulate request id for local testing.
		if (!original.containsKey(Key.RequestID.name())) original.put(Key.RequestID.name(), KeyUtils.objectId());
		KEY_LOCAL.set((String) original.get(Key.RequestID.name()));
		super.load(original);
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
