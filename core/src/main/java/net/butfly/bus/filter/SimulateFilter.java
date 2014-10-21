package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.util.XMLUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulateFilter extends FilterBase implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(SimulateFilter.class);
	private String source;

	@Override
	public void initialize(Map<String, String> params, Side side) {
		super.initialize(params, side);
		this.source = params.get("source");
		if (null == this.source) throw new SystemException(Constants.UserError.CONFIG_ERROR,
				"Parameter simulate-dir not found.");
		else logger.trace("Simulate albus transaction with files in: " + source);
	}

	@Override
	public Response execute(Request request) throws Exception {
		String filename = request.code() + "-" + request.version() + ".xml";
		try {
			return new Response(request).result(XMLUtils.parse(source + (source.endsWith("\\") ? "" : "\\") + filename));
		} catch (Throwable e) {
			throw new SystemException(Constants.BusinessError.SIMULATE_ERROR, e);
		}
	}
}
