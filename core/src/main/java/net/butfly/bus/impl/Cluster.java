package net.butfly.bus.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.butfly.bus.Bus;
import net.butfly.bus.Buses;
import net.butfly.bus.Mode;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.context.Contexts;
import net.butfly.bus.policy.Router;

class Cluster {
	protected static Logger logger = LoggerFactory.getLogger(Cluster.class);
	private final Map<String, Bus> nodes = new HashMap<String, Bus>();
	final private Mode mode;
	final private Router router;

	Cluster(Mode mode, Router router, String... conf) {
		this.mode = mode;
		this.router = router;
		if (null == conf || conf.length == 0) this.registerSingle(null);
		else for (String c : conf)
			if (!"".equals(c.trim())) this.registerSingle(c);
	}

	public Bus[] servers() {
		return nodes.values().toArray(new Bus[nodes.values().size()]);
	}

	private void registerSingle(String conf) {
		Bus impl = (Bus) BusFactory.create(mode, conf);
		nodes.put(impl.id(), impl);
	}

	public void invoking(Invoking invoking) {
		invoking.bus = router.route(invoking.tx.value(), servers());
		if (null == invoking.bus)
			throw new RuntimeException("Server routing failure, no node found for [" + invoking.tx.value() + "].");
		invoking.parameterClasses = ((BaseBus) invoking.bus).invokingMethod(invoking.tx).getParameterTypes();
	}

	public final Response invoke(final Invoking invoking) throws Exception {
		Contexts.initialize(Contexts.deserialize(invoking.context));
		Request req = Buses.request(invoking.tx, invoking.context, invoking.parameters);
		return ((BusImpl) invoking.bus).invoke(req, invoking.options);
	}
}
