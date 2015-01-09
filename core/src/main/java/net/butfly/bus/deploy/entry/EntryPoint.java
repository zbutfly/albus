package net.butfly.bus.deploy.entry;

import net.butfly.bus.Request;
import net.butfly.bus.Response;

public interface EntryPoint {
	public Response invoke(Request request) throws Exception;
}
