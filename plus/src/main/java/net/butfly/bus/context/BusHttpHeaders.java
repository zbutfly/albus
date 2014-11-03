package net.butfly.bus.context;

public interface BusHttpHeaders {
	String HEADER_PREFIX = "X-BUS-";
	String HEADER_TX_CODE = HEADER_PREFIX + "TX";
	String HEADER_TX_VERSION = HEADER_PREFIX + "TX-Version";
	String HEADER_CONTINUOUS = HEADER_PREFIX + "Continuous";
	String HEADER_CONTEXT_PREFIX = HEADER_PREFIX + "Context-";
}
