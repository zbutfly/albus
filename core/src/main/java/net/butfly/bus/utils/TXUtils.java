package net.butfly.bus.utils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.UtilsBase;
import net.butfly.bus.TX;

public class TXUtils extends UtilsBase {
	public static final TX TXImpl(final String tx) {
		return TXImpl(tx, null);
	}

	public static final TXImpl TXImpl(final TX tx) {
		return TXImpl(tx.value(), tx.version());
	}

	public static TXImpl TXImpl(final String code, final String version) {
		String ver = version == null ? TX.ALL_VERSION : version;
		String key = key(code, ver);
		if (TX_IMPL_POOL.containsKey(key)) return TX_IMPL_POOL.get(key);
		TXImpl impl = new TXImpl(code, ver);
		TX_IMPL_POOL.put(key, impl);
		return impl;
	}

	@SuppressWarnings("all")
	public static class TXImpl implements TX, Serializable, Comparable<TX> {
		private static final long serialVersionUID = 2457478261818103564L;

		public TXImpl(String code, String version) {
			super();
			this.code = code;
			this.version = null == version ? TX.ALL_VERSION : version;
		}

		public TXImpl(String key) {
			String[] keys = key.split(":");
			if (keys.length != 2) throw new IllegalArgumentException();
			this.code = keys[0];
			this.version = keys[1];
		}

		private String code;
		private String version;

		@Override
		public Class<? extends Annotation> annotationType() {
			return TX.class;
		}

		@Override
		public String value() {
			return code;
		}

		@Override
		public String version() {
			return version;
		}

		@Override
		public String toString() {
			return key(code, version);
		}

		@Override
		public boolean equals(Object tx) {
			if (tx == null || !TX.class.isAssignableFrom(tx.getClass())) return false;
			TX txx = (TX) tx;
			return txx.value().equals(this.code) && txx.version().equals(this.version);
		}

		public boolean matching(TX tx) {
			if (null == tx || !tx.value().equals(this.code)) return false;
			return this.version.compareTo(tx.value()) <= 0;
		}

		@Override
		public int compareTo(TX tx) {
			if (tx == null) return 1;
			if (code.equals(tx.value())) return version.compareTo(tx.version());
			return code.compareTo(tx.value());
		}
	}

	private static final Map<String, TXImpl> TX_IMPL_POOL = new HashMap<String, TXImpl>();

	public static final String key(TX tx) {
		return key(tx.value(), tx.version());
	}

	public static final String key(String code, String version) {
		return code + ":" + version;
	}

	public static final boolean isMatching(String[] patterns, String code) {
		for (String pattern : patterns) {
			if (pattern.equals("*")) return true;
			if (pattern.equalsIgnoreCase(code)) return true;
			if (pattern.endsWith("*") && code.startsWith(pattern.substring(0, pattern.indexOf("*")))) return true;
		}
		return false;
	}
}
