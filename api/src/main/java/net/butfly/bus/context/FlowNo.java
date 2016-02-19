package net.butfly.bus.context;

import java.io.Serializable;

public final class FlowNo implements Serializable, Cloneable {
	private static final long serialVersionUID = -3616807811490455640L;
	static final String DATE_FORMAT = "yyMMddHHmmssSSS";

	String code;
	String version;
	long timestamp;
	String serial;
	long sequence;

	protected FlowNo() {}
}
