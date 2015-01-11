package net.butfly.bus.impl;

import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.policy.Router;

public class EntryPointImpl implements EntryPoint {
	private Router router;
	private Cluster servers;

	public EntryPointImpl(Cluster servers, Router router) {
		this.servers = servers;
		this.router = router;
	}

	@Override
	public Response invoke(Request request) throws Exception {
		return router.route(request.code(), ((BusImpl[]) servers.servers())).invoke(request);
	}
}
