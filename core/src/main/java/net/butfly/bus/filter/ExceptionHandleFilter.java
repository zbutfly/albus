package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.util.BusUtils;
import net.butfly.bus.util.async.Signal;

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
	public Response execute(Request request) throws Exception {
		Response response = null;
		try {
			response = super.execute(request);
		} catch (Signal sig) {
			throw sig;
		} catch (Exception ex) {
			if (side == Side.SERVER) {
				response = new Response(request);
				BusUtils.exceptionToError(response, ex, debugging);
			} else throw ex;
		}
		return response;
	}

	@Override
	public void after(Request request, Response response) {
		if (response != null && side == Side.CLIENT) BusUtils.errorToException(response);
	}
}
