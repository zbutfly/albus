package net.butfly.bus.utils;

import java.lang.annotation.Annotation;

import net.butfly.bus.TX;

public final class BusUtils {
	private BusUtils() {}

	public static String getServiceKey(TX tx) {
		return getServiceKey(tx.value(), tx.version());
	}

	public static String getServiceKey(Annotation tx) {
		try {
			return getServiceKey((String) tx.getClass().getMethod("value").invoke(tx),
					(String) tx.getClass().getMethod("version").invoke(tx));
		} catch (Exception e) {
			return null;
		}
	}

	public static String getServiceKey(String txCode, String versionNo) {
		return txCode + "-" + versionNo;
	}
}
