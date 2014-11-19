package net.butfly.bus.context;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.butfly.bus.Request;
import net.butfly.bus.context.Context.Key;

import org.apache.commons.lang3.time.FastDateFormat;

public final class FlowNo implements Serializable, Cloneable {
	private static final long serialVersionUID = -3616807811490455640L;
	private static final FastDateFormat FLOWNO_FORMAT = FastDateFormat.getInstance("yyMMddHHmmssSSS");
	private static final DateFormat FLOWNO_UNFORMAT = new SimpleDateFormat("yyMMddHHmmssSSS");

	private String code;
	private String version;
	private long timestamp;
	private String serial;
	private long sequence;

	public FlowNo(Request request) {
		String fn = request.context(Key.FlowNo.name());
		FlowNo existed = null == fn ? Context.flowNo() : new FlowNo(fn);
		if (null == existed) {
			this.serial = random();
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
		if (fields.length != 4) throw new IllegalArgumentException();
		try {
			synchronized (FLOWNO_UNFORMAT) {
				this.timestamp = FLOWNO_UNFORMAT.parse(fields[0].substring(0, fields[0].length() - RANDOM_LENGTH)).getTime();
			}
			this.code = fields[2];
			this.version = fields[3];
			this.serial = fields[0].substring(fields[0].length() - RANDOM_LENGTH, fields[0].length());
			this.sequence = Long.parseLong(fields[1]);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public String toString() {
		String timestamp;
		timestamp = FLOWNO_FORMAT.format(new Date(this.timestamp));
		return new StringBuilder(timestamp).append(serial).append("#").append(sequence).append("@").append(code).append(":")
				.append(version).toString();
	}

	private static String RANDOM_SEED = "0123456789";
	static {
		RANDOM_SEED = "";
		for (int i = 0; i < 10; i++) {
			RANDOM_SEED = RANDOM_SEED + i;
		}
	}
	private static int RANDOM_LENGTH = 7;

	private static synchronized String random() {
		return random(RANDOM_LENGTH);
	}

	private static synchronized String random(int length) {
		StringBuilder rst = new StringBuilder();
		for (int i = 0; i < length; i++)
			rst.append(RANDOM_SEED.charAt((int) (Math.random() * RANDOM_SEED.length())));
		return rst.toString();
	}
}
