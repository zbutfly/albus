package net.butfly.bus.context;

public interface BusHttpHeaders {
	String HEADER_PREFIX = "X-BUS-";
	String HEADER_TX_CODE = HEADER_PREFIX + "TX";
	String HEADER_TX_VERSION = HEADER_PREFIX + "TX-Version";
	String HEADER_OPTIONS = HEADER_PREFIX + "Options";
	String HEADER_SUPPORT_CLASS = HEADER_PREFIX + "SupportClass";
	String HEADER_CONTEXT_PREFIX = HEADER_PREFIX + "Context-";
}
