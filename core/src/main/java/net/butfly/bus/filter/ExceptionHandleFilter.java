package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.utils.BusUtils;

public class ExceptionHandleFilter extends FilterBase implements Filter {
	private boolean debugging;

	@Override
	public void initialize(Map<String, String> params, Side side) {
		super.initialize(params, side);
		try {
			this.debugging = Boolean.valueOf(params.get("debug"));
		} catch (Throwable th) {
			this.debugging = false;
		}
	}

	@Override
	public void before(Request request) {}

	@Override
	public Response execute(Request request) throws Signal {
		Response response = null;
		try {
			response = super.execute(request);
		} catch (Signal signal) {
			throw signal;
		} catch (Exception ex) {
			if (side == Side.SERVER) {
				response = new Response(request);
				BusUtils.exceptionToError(response, ex, debugging);
			} else throw new Signal.Completed(ex);
		}
		return response;
	}

	@Override
	public void after(Request request, Response response) {
		if (response != null && side == Side.CLIENT) BusUtils.errorToException(response);
	}
}
