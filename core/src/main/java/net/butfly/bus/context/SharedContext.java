package net.butfly.bus.context;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class SharedContext extends Context {
	private Map<String, Object> context;

	public SharedContext() {
		super();
		context = new ConcurrentHashMap<String, Object>();
	}

	public String toString() {
		return context.toString();
	}

	@Override
	public int size() {
		return context.size();
	}

	@Override
	public boolean isEmpty() {
		return context.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return context.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return context.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return context.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return context.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return context.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		context.putAll(map);
	}

	@Override
	public void clear() {
		context.clear();
	}

	@Override
	public Set<String> keySet() {
		return context.keySet();
	}

	@Override
	public Collection<Object> values() {
		return context.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return context.entrySet();
	}

	@Override
	protected Map<String, Object> innerToMap() {
		return context;
	}
}
