package net.butfly.bus.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.argument.Error;

public final class BusUtils {
	private BusUtils() {}

	public static String getStackTrace(Throwable e) {
		// to avoid JDK BUG, u can not invoke e.printStackTrace directly.
		StringWriter s = new StringWriter();
		PrintWriter w = new PrintWriter(s);
		w.println(e);
		StackTraceElement[] trace = e.getStackTrace();
		for (int i = 0; i < trace.length; i++)
			w.println("\tat " + trace[i]);
		Throwable cause = e.getCause();
		if (cause != null && cause != e) {
			w.println("Caused by " + cause);
			w.println(cause);
		}
		return s.toString();
	}

	public static String getStackTrace() {
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		if (null == s || s.length < 2) return null;
		return s[1].toString();
	}

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

	public static String join(String[] strings, String delimiter) {
		if (null == strings || strings.length == 0) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < strings.length - 1; i++)
			sb.append(strings[i]).append(delimiter);
		sb.append(strings[strings.length - 1]);
		return sb.toString();
	}

	public static SystemException findBusExceptionInCause(Throwable ex) {
		while (null != ex) {
			if (ex instanceof SystemException) return (SystemException) ex;
			if (ex == ex.getCause()) return null;
			ex = ex.getCause();
		}
		return null;
	}

	public static Exception findOtherExceptionInCause(SystemException ex) {
		Throwable e = ex.getCause();
		while (null != e) {
			if (!(e instanceof SystemException)) return e instanceof Exception ? (Exception) e : new Exception(e);
			if (e == e.getCause()) return null;
			e = e.getCause();
		}
		return null;
	}

	public static void exceptionToError(Response response, Exception e, boolean debugging) {
		SystemException ee = BusUtils.findBusExceptionInCause(e);
		if (null != ee) response.error(new Error(ee, debugging));
		else response.error(new Error(e, debugging));
	}

	public static void errorToException(Response response) {
		Error err = response.error();
		if (null != err) throw exception(err);
	}

	private static SystemException exception(Error error) {
		if (null == error) return null;
		SystemException ex = new SystemException(error.getCode(), error.getMessage(), exception(error.getCause()));
		ex.setStackTrace(error.getStackTraces());
		return ex;
	}
}
