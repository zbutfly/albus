package net.butfly.bus.context;

import java.util.HashMap;
import java.util.Map;

import net.butfly.bus.argument.TX;
import net.butfly.bus.auth.Token;
import net.butfly.bus.util.TXUtils;

public abstract class Context implements Map<String, Object> {
	public static Context CURRENT = null;

	public enum Key {
		FlowNo, TXInfo, SourceAppID, SourceHost, TOKEN, USERNAME, PASSWORD;
	}

	private static boolean sharing() {
		return ThreadLocalContext.class.isAssignableFrom(CURRENT.getClass());
	}

	public static void folk(Context context) {
		if (!ThreadLocalContext.class.isAssignableFrom(CURRENT.getClass())) return;
		if (SharedContext.class.isAssignableFrom(context.getClass()))
			((ThreadLocalContext) CURRENT).initializeLocal((SharedContext) context);
		if (ThreadLocalContext.class.isAssignableFrom(context.getClass()))
			((ThreadLocalContext) CURRENT).initializeLocal(((ThreadLocalContext) context).context.get());
	}

	public static void initialize(boolean sharing) {
		if (sharing) {
			if (CURRENT == null || !sharing()) CURRENT = new SharedContext();
		} else {
			if (CURRENT == null || sharing()) CURRENT = new ThreadLocalContext();
			((ThreadLocalContext) CURRENT).initializeLocal(null);
		}
	}

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

	public static FlowNo flowNo() {
		return (FlowNo) CURRENT.get(Key.FlowNo);
	}

	public static String sourceAppID() {
		return (String) CURRENT.get(Key.SourceAppID);
	}

	public static String sourceHost() {
		return (String) CURRENT.get(Key.SourceHost);
	}

	public static TX txInfo() {
		return (TX) CURRENT.get(Key.TXInfo);
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
		CURRENT.put(Key.TXInfo.name(), (TXUtils.TXImpl) TXUtils.TXImpl(tx));
	}

	/****************************************************/

	public enum Mode {
		ReplacingByCleaning, MergingWithNew, MergingOnlyAbsent;
	}

	public static Map<String, Object> toMap() {
		return CURRENT.innerToMap();
	}

	protected abstract Map<String, Object> innerToMap();

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
				CURRENT.putIfAbsent(e.getKey(), e.getValue());
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
				dst.put(key.name(), new TXUtils.TXImpl(src.get(key.name())));
				continue;
			default:
				dst.put(key.name(), src.get(key.name()).toString());
				continue;
			}
		}
		return dst;
	}
}
