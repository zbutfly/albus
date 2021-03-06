package net.butfly.bus.impl;

import java.util.function.Consumer;

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

	public final void invoke(final Invoking invoking, Consumer<Response> callback) throws Exception {
		Contexts.initialize(Contexts.deserialize(invoking.context));
		Request req = Buses.request(invoking.tx, invoking.context, invoking.parameters);
		if (invoking.bus instanceof AsyncBusImpl) ((AsyncBusImpl) invoking.bus).invoke(req, callback, invoking.options);
		else if (invoking.bus instanceof BusImpl) callback.accept(((BusImpl) invoking.bus).invoke(req, invoking.options));
	}
}
