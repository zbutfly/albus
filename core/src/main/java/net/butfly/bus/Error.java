package net.butfly.bus;

import java.io.Serializable;

public interface Error extends Serializable {
	public String getCode();

	public String getMessage();

	public StackTraceElement[] getStackTraces();

	public Error getCause();

	public String toString();

	public String toString(int stackTraceLimit, int causeLimit);

	public Exception toException();
}
