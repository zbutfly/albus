package net.butfly.bus.filter;

import java.lang.reflect.Array;
import java.util.Map;

import net.butfly.albacore.utils.Objects;
import net.butfly.bus.Request;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;
import net.butfly.bus.service.LogService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerFilter extends FilterBase implements Filter {
	private final Logger logger = LoggerFactory.getLogger(LoggerFilter.class);

	public void execute(final FilterContext context) throws Exception {
		Request request = context.request();
		String prefix = null;
		long now = System.currentTimeMillis();
		if (logger.isInfoEnabled() || logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("BUS").append("[").append(request.code()).append(":")
					.append(request.version()).append("]");
			FlowNo fn = Context.flowNo();
			if (null != fn) sb.append("[").append(fn.toString()).append("]");
			sb.append(":");
			prefix = sb.toString();
		}

		if (logger.isTraceEnabled()) {
			logger.trace(prefix + " invoking begin...");
			StringBuilder sb = new StringBuilder();
			sb.append(" invoking options detail: ").append("\n\tcontext: ").append(Context.string()).append("\n\ttx code: ")
					.append(request.code()).append("\n\ttx version: ").append(request.version())
					.append("\n\trequest arguments: ");
			int ai = 1;
			if (request.arguments() != null) for (Object arg : request.arguments()) {
				sb.append("\n\t\t").append("[").append(ai++).append("]: ");
				this.printObject(sb, arg);
			}
			else sb.append("[NULL]");
			logger.trace(prefix + sb.toString());
		}
		try {
			LogService log = context.invoker().awared(LogService.class);
			if (null != log) log.logAccess();
			super.execute(context);
		} finally {
			if (logger.isInfoEnabled()) {
				long spent = System.currentTimeMillis() - now;
				logger.info(prefix + " invoking ended in [" + spent + "ms].");
			}
			if (null != context.response()) {
				if (context.response().error() != null) logger.error("Bus error: " + context.response().error().toString());
				if (logger.isTraceEnabled()) {
					StringBuilder sb = new StringBuilder();
					sb.append(" invoking response detail: ").append("\n\tcontext: ").append(Context.string())
							.append("\n\tresponse result: ");
					if (null != context.response()) this.printObject(sb, context.response().result());
					logger.trace(prefix + sb.toString());
				}
			}
		}
	}

	private void printObject(StringBuilder sb, Object obj) {
		if (null == obj) sb.append("[NULL]");
		else {
			Map<String, Object> map = Objects.toMap(obj);
			shrink(map);
			sb.append("[").append(obj.getClass().getName()).append("]").append(":").append(map.toString());
		}
	}

	private static int MAX_ARRAY_LENGTH = 10;
	private static int MAX_STRING_LENGTH = 50;

	private void shrink(Map<String, Object> map) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object v = entry.getValue();
			if (v == null) continue;
			Class<? extends Object> vc = v.getClass();
			if (String.class.isAssignableFrom(vc) && ((String) v).length() > MAX_STRING_LENGTH)
				map.put(entry.getKey(), ((String) v).substring(0, MAX_STRING_LENGTH));
			if (vc.isArray() && Array.getLength(v) > MAX_ARRAY_LENGTH) {
				Class<?> vt = vc.getComponentType();
				if (vt.isPrimitive() || String.class.isAssignableFrom(vt) || Number.class.isAssignableFrom(vt)
						|| Boolean.class.isAssignableFrom(vt) || Character.class.isAssignableFrom(vt)) {
					Object newv = Array.newInstance(vt, MAX_ARRAY_LENGTH);
					System.arraycopy(v, 0, newv, 0, MAX_ARRAY_LENGTH);
					map.put(entry.getKey(), newv);
				}
			}
		}
	}
}
