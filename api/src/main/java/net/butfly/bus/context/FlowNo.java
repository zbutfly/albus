package net.butfly.bus.context;

import java.io.Serializable;
import java.util.Date;

import net.butfly.albacore.utils.Keys;
import net.butfly.albacore.utils.Texts;
import net.butfly.bus.Request;
import net.butfly.bus.context.Context.Key;

public final class FlowNo implements Serializable, Cloneable {
	private static final long serialVersionUID = -3616807811490455640L;
	private static final String DATE_FORMAT = "yyMMddHHmmssSSS";

	private String code;
	private String version;
	private long timestamp;
	private String serial;
	private long sequence;

	public FlowNo(Request request) {
		String fn = request.context(Key.FlowNo.name());
		FlowNo existed = null == fn ? Context.flowNo() : new FlowNo(fn);
		if (null == existed) {
			this.serial = Keys.key(String.class);
			this.sequence = 1;
			this.timestamp = new Date().getTime();
		} else {
			this.serial = existed.serial;
			this.sequence = existed.sequence + (null == fn ? 1 : 0);
			this.timestamp = existed.timestamp;
		}
		this.code = request.code();
		this.version = request.version();
		Context.flowNo(this);
	}

	public FlowNo(String flowno) {
		if (null == flowno) throw new IllegalArgumentException();
		String[] fields = flowno.split("[#@:]");
		if (fields.length != 5) throw new IllegalArgumentException();
		try {
			this.timestamp = Texts.parseDate(DATE_FORMAT, fields[0]).getTime();
			this.code = fields[3];
			this.version = fields[4];
			this.serial = fields[1];
			this.sequence = Long.parseLong(fields[2]);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public String toString() {
		String timestamp;
		timestamp = Texts.formatDate(DATE_FORMAT, new Date(this.timestamp));
		return new StringBuilder(timestamp).append("#").append(serial).append("#").append(sequence).append("@").append(code).append(":")
				.append(version).toString();
	}

}
