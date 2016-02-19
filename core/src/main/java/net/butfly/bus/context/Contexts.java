package net.butfly.bus.context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.Keys;
import net.butfly.albacore.utils.Texts;
import net.butfly.bus.Request;
import net.butfly.bus.TXs;
import net.butfly.bus.context.Context.Key;

public interface Contexts {
	public static void initialize(Map<String, Object> original) {
		if (Context.CURRENT == null) Context.CURRENT = new RequestContext();
		Context.CURRENT.load(original);
	}

	public static void clean(Map<String, Object> original) {
		if (Context.CURRENT == null) Context.CURRENT = new RequestContext();
		Context.CURRENT.load(original);
	}

	public static Map<String, Object> deserialize(Map<String, String> src) {
		Map<String, Object> dst = new HashMap<String, Object>();
		for (Key key : Key.values()) {
			if (src.containsKey(key.name())) switch (key) {
			case FlowNo:
				dst.put(key.name(), flowNo(src.get(key.name())));
				continue;
			case TXInfo:
				dst.put(key.name(), TXs.impl(src.get(key.name())));
				continue;
			default:
				dst.put(key.name(), src.get(key.name()).toString());
				continue;
			}
		}
		return dst;
	}

	public static FlowNo flowNo(Request request) {
		String fn = request.context(Key.FlowNo.name());
		FlowNo existed = null == fn ? Context.flowNo() : flowNo(fn);
		FlowNo fno = new FlowNo();
		if (null == existed) {
			fno.serial = Keys.key(String.class);
			fno.sequence = 1;
			fno.timestamp = new Date().getTime();
		} else {
			fno.serial = existed.serial;
			fno.sequence = existed.sequence + (null == fn ? 1 : 0);
			fno.timestamp = existed.timestamp;
		}
		fno.code = request.code();
		fno.version = request.version();
		Context.flowNo(fno);
		return fno;
	}

	public static FlowNo flowNo(String flowno) {
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

	public static String format(FlowNo flowno) {
		String timestamp = Texts.dateFormat(FlowNo.DATE_FORMAT).format(new Date(flowno.timestamp));
		return new StringBuilder(timestamp).append("#").append(flowno.serial).append("#").append(flowno.sequence).append("@")
				.append(flowno.code).append(":").append(flowno.version).toString();
	}
}
