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
	public void before(Request request) {
		StringBuilder sb = new StringBuilder("BUS").append("[").append(request.code()).append(":").append(request.version())
				.append("]").append("[").append(side.name()).append("]");
		FlowNo fn = Context.flowNo();
		if (null != fn) sb.append("[").append(fn.toString()).append("]");
		sb.append(":");
		long now = System.currentTimeMillis();
		String prefix = sb.toString();
		logger.trace(prefix + " invoking begin...");
		logger.trace(prefix + getDebugDetail(request));
		this.putParams(request, new Object[] { now, prefix });
	}

	@Override
	public void after(Request request, Response response) {
		Object[] params = this.getParams(request);
		long now = (Long) params[0];
		String prefix = (String) params[1];
		long spent = System.currentTimeMillis() - now;
		if (null != response && response.error() != null) logger.error("Bus error: \n" + response.error().toString());
		logger.info(prefix + " invoking ended in [" + spent + "ms].");
		logger.trace(prefix + getDebugDetail(response));
	}

	private String getDebugDetail(Response response) {
		StringBuilder sb = new StringBuilder();
		sb.append(" invoking response detail: ").append("\n\tcontext: ").append(Context.string())
				.append("\n\tresponse result: ");
		if (response != null && response.result() != null) {
			String result = response.result().toString();
			sb.append("[").append(response.result().getClass().getName()).append("]").append("\n\t\t").append(result);
		} else sb.append("[NULL]");
		return sb.toString();
	}

	private String getDebugDetail(Request request) {
		StringBuilder sb = new StringBuilder();
		sb.append(" invoking options detail: ").append("\n\tcontext: ").append(Context.string()).append("\n\ttx code: ")
				.append(request.code()).append("\n\ttx version: ").append(request.version()).append("\n\trequest arguments: ");
		int ai = 1;
		if (request.arguments() != null) for (Object arg : request.arguments()) {
			sb.append("\n\t\t").append("[").append(ai++).append("]: ");
			if (null != arg) sb.append(arg).append("\n\t\t\t").append("[").append(arg.getClass().getName()).append("]");
			else sb.append("[NULL]");
		}
		else sb.append("[NULL]");
		return sb.toString();
	}
}
