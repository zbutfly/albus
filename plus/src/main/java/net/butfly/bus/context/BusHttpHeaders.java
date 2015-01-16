package net.butfly.bus.context;

public interface BusHttpHeaders {
	String HEADER_PREFIX = "X-BUS-";
	String HEADER_TX_CODE = HEADER_PREFIX + "TX";
	String HEADER_TX_VERSION = HEADER_PREFIX + "TX-Version";
	String HEADER_OPTIONS = HEADER_PREFIX + "Options";
	String HEADER_CLASS = HEADER_PREFIX + "Class";
	String HEADER_CLASS_SUPPORT = HEADER_PREFIX + "Class-Supportted";
	String HEADER_CONTEXT_PREFIX = HEADER_PREFIX + "Context-";
	// Request ID in Response Header
	String HEADER_REQUEST_ID = HEADER_PREFIX + "Request-Id";
	String HEADER_ERROR = HEADER_PREFIX + "Error";
	String HEADER_ERROR_DETAIL = HEADER_PREFIX + "Error-Detail";
}
