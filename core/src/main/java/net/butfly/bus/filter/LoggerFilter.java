package net.butfly.bus.filter;

import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerFilter extends FilterBase implements Filter {
	private final Logger logger = LoggerFactory.getLogger(LoggerFilter.class);

	@Override
	public void before(FilterContext context) {
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
		context.param("now", now);
		context.param("prefix", prefix);

		if (logger.isTraceEnabled()) {
			logger.trace(prefix + " invoking begin...");
			logger.trace(prefix + getDebugDetail(request));
		}
	}

	@Override
	public void after(FilterContext context) {
		String prefix = this.print(context);
		Response response = context.response();
		if (response.error() != null) logger.error("Bus error: " + response.error().toString());
		if (logger.isTraceEnabled()) logger.trace(prefix + getDebugDetail(response));
	}

	@Override
	public Response exception(FilterContext context, Exception exception) throws Exception {
		this.print(context);
		if (null != exception) logger.error("Bus exception: ", exception);
		return super.exception(context, exception);
	}

	private String print(FilterContext context) {
		String prefix = context.param("prefix");
		if (logger.isInfoEnabled()) {
			long spent = System.currentTimeMillis() - (Long) context.param("now");
			logger.info(prefix + " invoking ended in [" + spent + "ms].");
		}
		return prefix;
	}

	private String getDebugDetail(Response response) {
		StringBuilder sb = new StringBuilder();
		sb.append(" invoking response detail: ").append("\n\tcontext: ").append(Context.string())
				.append("\n\tresponse result: ");
		if (null != response) this.printObject(sb, response.result());
		return sb.toString();
	}

	private String getDebugDetail(Request request) {
		StringBuilder sb = new StringBuilder();
		sb.append(" invoking options detail: ").append("\n\tcontext: ").append(Context.string()).append("\n\ttx code: ")
				.append(request.code()).append("\n\ttx version: ").append(request.version()).append("\n\trequest arguments: ");
		int ai = 1;
		if (request.arguments() != null) for (Object arg : request.arguments()) {
			sb.append("\n\t\t").append("[").append(ai++).append("]: ");
			this.printObject(sb, arg);
		}
		else sb.append("[NULL]");
		return sb.toString();
	}

	private void printObject(StringBuilder sb, Object obj) {
		if (null != obj) sb.append("[").append(obj.getClass().getName()).append("]").append(":").append(obj.toString());
		else sb.append("[NULL]");
	}
}
