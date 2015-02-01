package net.butfly.bus;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import net.butfly.albacore.utils.Utils;

public class TXs extends Utils {
	public static final TX impl(final String... tx) {
		switch (tx.length) {
		case 0:
			return null;
		case 1:
			return doimpl(tx[0], TX.ALL_VERSION);
		default:
			return doimpl(tx[0], null == tx[1] ? TX.ALL_VERSION : tx[1]);
		}
	}

	private static TX doimpl(final String code, final String version) {
		return new TXImpl(code, version);
	}

	@SuppressWarnings("all")
	private static class TXImpl implements TX, Serializable, Comparable<TX> {
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

	public static final String key(TX tx) {
		return key(tx.value(), tx.version());
	}

	public static final String key(String code, String version) {
		return code + ":" + version;
	}

	public static final int matching(String code, String... parttern) {
		for (int i = 0; i < parttern.length; i++) {
			String pattern = parttern[i];
			if (pattern.equals("*")) return i;
			if (pattern.equalsIgnoreCase(code)) return i;
			if (pattern.endsWith("*") && code.startsWith(pattern.substring(0, pattern.indexOf("*")))) return i;
		}
		return -1;
	}
}
