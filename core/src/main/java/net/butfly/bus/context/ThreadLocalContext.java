package net.butfly.bus.context;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ThreadLocalContext extends Context {
	InheritableThreadLocal<SharedContext> context;

	public ThreadLocalContext() {
		super();
		context = new InheritableThreadLocal<SharedContext>();
	}

	void initializeLocal(SharedContext context) {
		if (context == null) {
			SharedContext l = this.context.get();
			if (l == null) {
				l = new SharedContext();
				this.context.set(l);
			}
		} else this.context.set(context);
	}

	public String toString() {
		return context.get().toString();
	}

	@Override
	public int size() {
		return context.get().size();
	}

	@Override
	public boolean isEmpty() {
		return context.get().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return context.get().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return context.get().containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return context.get().get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return context.get().put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return context.get().remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		context.get().putAll(m);
	}

	@Override
	public void clear() {
		context.get().clear();
	}

	@Override
	public Set<String> keySet() {
		return context.get().keySet();
	}

	@Override
	public Collection<Object> values() {
		return context.get().values();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return context.get().entrySet();
	}

	@Override
	protected Map<String, Object> innerToMap() {
		return context.get().innerToMap();
	}
}
