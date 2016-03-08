package net.butfly.bus.impl;

import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Buses;
import net.butfly.bus.Mode;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.context.Contexts;
import net.butfly.bus.policy.Router;

public class AsyncCluster extends Cluster {
	AsyncCluster(Mode mode, Router router, String[] conf) {
		super(mode, router, conf);
	}

	public final void invoke(final Invoking invoking, Task.Callback<Response> callback) throws Exception {
		Contexts.initialize(Contexts.deserialize(invoking.context));
		Request req = Buses.request(invoking.tx, invoking.context, invoking.parameters);
		((AsyncBusImpl) invoking.bus).invoke(req, callback, invoking.options);
	}
}
