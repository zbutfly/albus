package net.butfly.bus.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.butfly.bus.TX;
import net.butfly.bus.TXs;
import net.butfly.bus.Token;

public abstract class Context implements Map<String, Object> {
	private static Context CURRENT = null;

	public enum Key {
		FlowNo, TXInfo, SourceAppID, SourceHost, TOKEN, USERNAME, PASSWORD, RequestID, Debug;
		private static final String TEMP_PREFIX = "_Inner";
	}

	public static String string() {
		return CURRENT.toString();
	}

	public static void initialize(Map<String, Object> original) {
		if (CURRENT == null) CURRENT = new RequestContext();
		CURRENT.load(original);
	}

	public static void clean(Map<String, Object> original) {
		if (CURRENT == null) CURRENT = new RequestContext();
		CURRENT.load(original);
	}

	// ***********************************************************************/
	public static void untoken() {
		CURRENT.remove(Key.TOKEN.name());
		CURRENT.remove(Key.USERNAME.name());
		CURRENT.remove(Key.PASSWORD.name());
	}

	public static void token(Token token) {
		untoken();
		if (null != token) {
			if (null != token.getKey()) CURRENT.put(Key.TOKEN.name(), token.getKey());
			else {
				CURRENT.put(Key.USERNAME.name(), token.getUsername());
				CURRENT.put(Key.PASSWORD.name(), token.getPassword());
			}
		}
	}

	public static Token token() {
		String key = (String) CURRENT.get(Key.TOKEN.name());
		if (null != key) return new Token(key);
		String pass = (String) CURRENT.get(Key.PASSWORD.name());
		String user = (String) CURRENT.get(Key.USERNAME.name());
		if (null != pass && null != user) return new Token(user, pass);
		return null;
	}

	public static boolean debug() {
		String debug = (String) CURRENT.get(Key.Debug.name());
		return (null != debug && Boolean.parseBoolean(debug));
	}

	public static FlowNo flowNo() {
		return (FlowNo) CURRENT.get(Key.FlowNo.name());
	}

	public static String sourceAppID() {
		return (String) CURRENT.get(Key.SourceAppID.name());
	}

	public static String sourceHost() {
		return (String) CURRENT.get(Key.SourceHost.name());
	}

	public static TX txInfo() {
		return (TX) CURRENT.get(Key.TXInfo.name());
	}

	public static void debug(boolean debug) {
		CURRENT.put(Key.Debug.name(), Boolean.toString(debug));
	}

	public static void flowNo(FlowNo flowNo) {
		CURRENT.put(Key.FlowNo.name(), flowNo);
	}

	public static void sourceAppID(String sourceAppID) {
		CURRENT.put(Key.SourceAppID.name(), sourceAppID);
	}

	public static void sourceHost(String sourceHost) {
		CURRENT.put(Key.SourceHost.name(), sourceHost);
	}

	public static void txInfo(TX tx) {
		CURRENT.put(Key.TXInfo.name(), tx);
	}

	public static void temp(String key, Object value) {
		CURRENT.put(Key.TEMP_PREFIX + key, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T temp(String key) {
		return (T) CURRENT.get(Key.TEMP_PREFIX + key);
	}

	/****************************************************/

	private enum Mode {
		ReplacingByCleaning, MergingWithNew, MergingOnlyAbsent;
	}

	public static Map<String, Object> toMap() {
		return CURRENT.impl();
	}

	public static void merge(Map<String, Object> src) {
		merge(src, Mode.MergingWithNew);
	}

	public static void merge(Map<String, Object> src, Mode mode) {
		if (src == null) return;
		switch (mode) {
		case ReplacingByCleaning:
			CURRENT.clear();
			CURRENT.putAll(src);
			return;
		case MergingWithNew:
			CURRENT.putAll(src);
			return;
		case MergingOnlyAbsent:
			for (Entry<String, Object> e : src.entrySet())
				if (!CURRENT.containsKey(e.getKey())) CURRENT.put(e.getKey(), e.getValue());
			return;
		}
	}

	public static Map<String, String> serialize(Map<String, Object> src) {
		Map<String, String> dst = new HashMap<String, String>();
		for (Key key : Key.values())
			if (src.containsKey(key.name())) dst.put(key.name(), src.get(key.name()).toString());
		return dst;
	}

	public static Map<String, Object> deserialize(Map<String, String> src) {
		Map<String, Object> dst = new HashMap<String, Object>();
		for (Key key : Key.values()) {
			if (src.containsKey(key.name())) switch (key) {
			case FlowNo:
				dst.put(key.name(), new FlowNo(src.get(key.name())));
				continue;
			case TXInfo:
				Object tx = src.get(key.name());
				if (tx instanceof TX) dst.put(key.name(), (TX) tx);
				else dst.put(key.name(), TXs.impl(tx.toString().split(":")));
				continue;
			default:
				dst.put(key.name(), src.get(key.name()).toString());
				continue;
			}
		}
		return dst;
	}

	/*********************************************************/
	@Override
	public String toString() {
		return impl().toString();
	}

	@Override
	public int size() {
		return impl().size();
	}

	@Override
	public boolean isEmpty() {
		return impl().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return impl().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return impl().containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return impl().get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return impl().put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return impl().remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		impl().putAll(m);
	}

	@Override
	public Set<String> keySet() {
		return impl().keySet();
	}

	@Override
	public Collection<Object> values() {
		return impl().values();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return impl().entrySet();
	}

	@Override
	public void clear() {
		impl().clear();
	}

	/************************************************/
	protected abstract Context current();

	protected abstract boolean sharing();

	protected void load(Map<String, Object> original) {
		if (null != original) impl().putAll(original);
	}

	protected abstract Map<String, Object> impl();
}
