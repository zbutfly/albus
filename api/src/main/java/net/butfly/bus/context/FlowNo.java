package net.butfly.bus.context;

import java.io.Serializable;

import net.butfly.albacore.utils.Texts;

public final class FlowNo implements Serializable, Cloneable {
	private static final long serialVersionUID = -3616807811490455640L;
	static final String DATE_FORMAT = "yyMMddHHmmssSSS";

	String code;
	String version;
	long timestamp;
	String serial;
	long sequence;

	protected FlowNo() {}

	@Override
	public String toString() {
		return new StringBuilder().append(timestamp).append("#").append(serial).append("#").append(sequence).append("@").append(code)
				.append(":").append(version).toString();
	}

	static FlowNo parse(String flowno) {
		if (null == flowno) throw new IllegalArgumentException();
		String[] fields = flowno.split("[#@:]");
		if (fields.length != 5) throw new IllegalArgumentException();
		FlowNo fno = new FlowNo();
		try {
			fno.timestamp = Texts.dateFormat(FlowNo.DATE_FORMAT).parse(fields[0]).getTime();
			fno.code = fields[3];
			fno.version = fields[4];
			fno.serial = fields[1];
			fno.sequence = Long.parseLong(fields[2]);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return fno;
	}
}
