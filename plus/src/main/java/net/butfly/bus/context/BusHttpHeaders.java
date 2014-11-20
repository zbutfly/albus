package net.butfly.bus.context;

public interface BusHttpHeaders {
	String HEADER_PREFIX = "X-BUS-";
	String HEADER_TX_CODE = HEADER_PREFIX + "TX";
	String HEADER_TX_VERSION = HEADER_PREFIX + "TX-Version";
	String HEADER_CONTINUOUS = HEADER_PREFIX + "Continuous";
	String HEADER_CONTINUOUS_PARAMS = HEADER_PREFIX + "ContinuousParams";
	String HEADER_SUPPORT_CLASS = HEADER_PREFIX + "SupportClass";
	String HEADER_CONTEXT_PREFIX = HEADER_PREFIX + "Context-";
}
