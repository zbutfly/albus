package net.butfly.bus.deploy.entry;

import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;


public interface EntryPoint {
	public Response invoke(Request request) throws Signal;
}
