package net.butfly.bus;

import java.io.Serializable;
import java.util.Arrays;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.ExceptionUtils;

public class Error implements Serializable {
	private static final long serialVersionUID = -8461232615545890309L;

	private String code;
	private String message;
	private Error cause;
	private StackTraceElement[] stackTrace;

	public Error(Throwable ex, boolean debugging) {
		this(ExceptionUtils.unlink(ex), debugging);
	}

	private Error(Throwable[] list, boolean debugging) {
		if (list.length == 0) throw new RuntimeException("Error should be null");
		Throwable ex = list[0];
		if (ex instanceof SystemException) this.code = ((SystemException) ex).getCode();
		else if (ex instanceof BusinessException) this.code = ((BusinessException) ex).getCode();
		else this.code = null;

		this.message = ex.getMessage();
		this.stackTrace = debugging ? ex.getStackTrace() : new StackTraceElement[0];
		if (list.length == 1) this.cause = null;
		else this.cause = new Error(Arrays.copyOfRange(list, 1, list.length), debugging);
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
		return toString(8, -1);
	}

	public String toString(int stackTraceLimit, int causeLimit) {
		int l = stackTraceLimit;
		StringBuilder sb = new StringBuilder();
		sb.append(this.getMessage()).append("[Code: ").append(this.getCode()).append("]");
		if (this.getStackTraces() != null) for (StackTraceElement st : this.getStackTraces()) {
			sb.append("\n").append("\tat ").append(st.toString());
			if (l-- == 0) {
				sb.append("\n(more......)");
				break;
			}
		}
		if (causeLimit != 0) {
			Error c = this.getCause();
			if (null != c) sb.append("\nCaused by:\n").append(c.toString(stackTraceLimit, --causeLimit));
		}
		return sb.toString();
	}

	public Exception toException() {
		SystemException ex = new SystemException(this.code, this.message, null == this.cause ? null : this.cause.toException());
		ex.setStackTrace(this.getStackTraces());
		return ex;
	}
}
