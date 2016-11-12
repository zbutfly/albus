package net.butfly.bus.context;

import java.io.Serializable;
import java.util.Date;

import net.butfly.albacore.utils.Texts;

public final class FlowNo implements Serializable, Cloneable {
	private static final long serialVersionUID = -3616807811490455640L;
	static final String DATE_FORMAT = "yyMMddHHmmssSSS";

	String code;
	String version;
	long timestamp;
	String serial;
	long sequence;

	FlowNo() {}

	static FlowNo parse(String flowno) {
		if (null == flowno) throw new IllegalArgumentException();
		String[] fields = flowno.split("[#@:]");
		if (fields.length != 5) throw new IllegalArgumentException();
		FlowNo fn = new FlowNo();
		try {
			fn.timestamp = Texts.dateFormat(DATE_FORMAT).parse(fields[0]).getTime();
			fn.code = fields[3];
			fn.version = fields[4];
			fn.serial = fields[1];
			fn.sequence = Long.parseLong(fields[2]);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return fn;
	}

	@Override
	public String toString() {
		String timestamp;
		timestamp = Texts.dateFormat(DATE_FORMAT).format(new Date(this.timestamp));
		return new StringBuilder(timestamp).append("#").append(serial).append("#").append(sequence).append("@").append(code).append(":")
				.append(version).toString();
	}

}
