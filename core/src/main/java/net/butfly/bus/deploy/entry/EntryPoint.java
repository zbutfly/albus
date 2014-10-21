package net.butfly.bus.deploy.entry;

import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;

public interface EntryPoint {
	public Response invoke(Request request);
}
