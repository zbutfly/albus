package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.albacore.utils.ExceptionUtils;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.argument.Constants.Side;
import net.butfly.bus.argument.Error;

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
	public Response execute(Request request) throws Exception {
		Response response = null;
		try {
			response = super.execute(request);
		} catch (Exception ex) {
			ex = ExceptionUtils.unwrap(ex);
			if (side == Side.SERVER) { // wrap error into response
				response = new Response(request);
				response.error(new Error(ex, debugging));
			} else throw ex;
		}
		if (side != Side.SERVER && response.error() != null) throw response.error().toException();
		return response;
	}
}
