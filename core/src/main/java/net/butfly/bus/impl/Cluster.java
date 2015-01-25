package net.butfly.bus.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.Reflections.MethodInfo;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;
import net.butfly.bus.impl.BusFactory.Mode;
import net.butfly.bus.policy.Routeable;
import net.butfly.bus.policy.Router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Cluster implements Routeable {
	private static Logger logger = LoggerFactory.getLogger(Cluster.class);
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

	@Override
	public String id() {
		return null;
	}

	@Override
	public String[] supportedTXs() {
		Set<String> all = new HashSet<String>();
		for (Bus impl : nodes.values())
			all.addAll(Arrays.asList(impl.supportedTXs()));
		return all.toArray(new String[all.size()]);
	}

	private void registerSingle(String conf) {
		Bus impl = (Bus) BusFactory.create(mode, conf);
		nodes.put(impl.id(), impl);
	}

	public void invoking(Invoking invoking) {
		invoking.bus = router.route(invoking.tx.value(), servers());
		if (null == invoking.bus)
			throw new RuntimeException("Server routing failure, no node found for [" + invoking.tx.value() + "].");
		MethodInfo pi = ((BasicBusImpl) invoking.bus).invokeInfo(invoking.tx);
		if (null == pi) throw new RuntimeException("Server routing failure.");
		invoking.parameterClasses = pi.parametersClasses();
	}

	public final void invoke(final Invoking invoking, Task.Callback<Response> callback) {
		Context.initialize(Context.deserialize(invoking.context));
		Request req = new Request(invoking.tx, invoking.context, invoking.parameters);
		try {
			((BusImpl) invoking.bus).invoke(req, callback, invoking.options);
		} catch (Exception e) {
			e = Exceptions.unwrap(e);
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}
