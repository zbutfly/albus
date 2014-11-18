package net.butfly.bus.deploy.entry;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.policy.Router;
import net.butfly.bus.utils.ServerWrapper;

public class EntryPointImpl implements EntryPoint {
	private Router router;
	private ServerWrapper servers;

	public EntryPointImpl(ServerWrapper servers, Router router) {
		this.servers = servers;
		this.router = router;
	}

	@Override
	public Response invoke(Request request) {
		try {
			return this.router.route(request.code(), servers.servers()).invoke(request);
		} catch (Signal signal) {
			throw signal;
		} catch (SystemException ex) {
			throw ex;
		} catch (Exception e) {
			throw new SystemException("", e);
		}
	}
}
