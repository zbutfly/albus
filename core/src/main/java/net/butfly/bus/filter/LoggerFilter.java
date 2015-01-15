package net.butfly.bus.filter;

import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.FlowNo;
import net.butfly.bus.utils.RequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerFilter extends FilterBase implements Filter {
	private final Logger logger = LoggerFactory.getLogger(LoggerFilter.class);

	@Override
	public void before(RequestWrapper<?> request) {
		Request req = request.request();
		String prefix = null;
		long now = System.currentTimeMillis();
		if (logger.isInfoEnabled() || logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("BUS").append("[").append(req.code()).append(":").append(req.version())
					.append("]");
			FlowNo fn = Context.flowNo();
			if (null != fn) sb.append("[").append(fn.toString()).append("]");
			sb.append(":");
			prefix = sb.toString();
		}
		this.putParams(request, now, prefix);

		if (logger.isTraceEnabled()) {
			logger.trace(prefix + " invoking begin...");
			logger.trace(prefix + getDebugDetail(req));
		}
	}

	@Override
	public void after(RequestWrapper<?> request, Response response) {
		Object[] params = this.getParams(request);
		String prefix = (String) params[1];
		if (logger.isInfoEnabled()) {
			long spent = System.currentTimeMillis() - (Long) params[0];
			logger.info(prefix + " invoking ended in [" + spent + "ms].");
		}
		if (null != response && response.error() != null) logger.error("StandardBus error: " + response.error().toString());
		if (logger.isTraceEnabled()) logger.trace(prefix + getDebugDetail(response));
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
