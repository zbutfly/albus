package net.butfly.bus.argument;

import java.io.Serializable;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.exception.SystemException;

public class Error implements Serializable {
	private static final long serialVersionUID = -8461232615545890309L;

	private String code = "";
	private String message;
	private Error cause = null;
	private StackTraceElement[] stackTrace;

	public Error(Throwable ex, boolean debugging) {
		if (ex instanceof SystemException) this.code = ((SystemException) ex).getCode();
		else if (ex instanceof BusinessException) this.code = ((BusinessException) ex).getCode();

		this.message = ex.getMessage();
		this.stackTrace = debugging ? null : ex.getStackTrace();
		if (ex.getCause() != null && !ex.getCause().equals(ex)) this.cause = new Error(ex.getCause(), debugging);
	}

	public Error(Throwable ex) {
		this(ex, true);
	}

	public Error(String code, String message) {
		this(code, message, null);
	}

	public Error(String code, String message, Error cause) {
		this.code = code;
		this.message = message;
		this.cause = cause;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public StackTraceElement[] getStackTraces() {
		return stackTrace;
	}

	public Error getCause() {
		return cause;
	}

	public String toString() {
		return toString(8);
	}

	public String toString(int stackTraceLines) {
		int l = 0;
		StringBuilder sb = new StringBuilder();
		sb.append("Bus error [").append(this.getCode()).append("]: ").append(this.getMessage()).append("\n");
		if (this.getStackTraces() != null) for (StackTraceElement st : this.getStackTraces()) {
			sb.append("\t").append(st.toString()).append("\n");
			if (l++ >= stackTraceLines) {
				sb.append("(more......)\n");
				break;
			}
		}
		Error c = this.getCause();
		if (null != c && this != c) sb.append("Caused by:").append("\n").append(c.toString());
		return sb.toString();
	}
}
