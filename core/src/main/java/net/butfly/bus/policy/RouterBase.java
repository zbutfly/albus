package net.butfly.bus.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.bus.util.TXUtils;

public abstract class RouterBase implements Router {
	protected final Map<String, Routeable[]> cache;

	public RouterBase() {
		this.cache = new HashMap<String, Routeable[]>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends Routeable> T route(String requestTX, T[] possiable) {
		if (possiable == null || possiable.length == 0) return null;
		// XXX: for global route cache.
		// Class<T> clazz = (Class<T>) possiable.getClass().getComponentType();
		// String key = requestTX + clazz.getName();
		Routeable[] target;
		if (this.cache.containsKey(requestTX)) target = this.cache.get(requestTX);
		else {
			Set<Routeable> results = new HashSet<Routeable>();
			if (possiable != null && possiable.length > 0) for (T et : possiable)
				if (TXUtils.isMatching(et.supportedTXs(), requestTX)) results.add(et);

			target = results.toArray(new Routeable[results.size()]);
		}
		return (T) this.route(target);
	}

	protected abstract <T> T route(T[] filted);
}
