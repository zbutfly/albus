package net.butfly.bus.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.butfly.bus.Request;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;
import net.butfly.bus.service.LogService;

public class LoggerFilter extends FilterBase implements Filter {
	private final Logger logger = LoggerFactory.getLogger(LoggerFilter.class);

	public void execute(final FilterContext context) throws Exception {
		Request request = context.request();
		String prefix = null;
		long now = System.currentTimeMillis();
		if (logger.isInfoEnabled() || logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("BUS").append("[").append(request.code()).append(":").append(request.version())
					.append("]");
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
			sb.append("[").append(obj.getClass().getName()).append("]").append(":").append(shrink(obj));
		}
	}

	public static String shrink(Object obj) {
		if (Context.debug()) return obj.toString();
		if (obj instanceof String) return ((String) obj).length() > MAX_STRING_LENGTH
				? ((String) obj).substring(0, MAX_STRING_LENGTH).replaceAll("\n", "") + "...[eliminated]" : ((String) obj);
		if (obj instanceof Number || obj instanceof Character || obj instanceof Boolean) return obj.toString();
		else return "[too long object eliminated: " + obj.getClass() + "]";
	}

	static int MAX_ARRAY_LENGTH = 10;
	private static int MAX_STRING_LENGTH = 250;
}
