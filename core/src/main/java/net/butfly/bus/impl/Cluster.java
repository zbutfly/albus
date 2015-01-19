package net.butfly.bus.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.ReflectionUtils.MethodInfo;
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

	Cluster(String[] config, Mode mode, Router router) {
		this.mode = mode;
		this.router = router;
		if (config == null || config.length == 0) this.registerSingle(null);
		else for (String conf : config)
			if (!"".equals(conf.trim())) this.registerSingle(conf);
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
		Bus impl = (Bus) BusFactory.create(conf, mode);
		nodes.put(impl.id(), impl);
	}

	public void invoking(Invoking invoking) {
		invoking.bus = router.route(invoking.tx.value(), servers());
		if (null == invoking.bus)
			throw new RuntimeException("Server routing failure, no node found for [" + invoking.tx.value() + "].");
		MethodInfo pi = ((BasicBusImpl) invoking.bus).invokeInfo(invoking.tx.value(), invoking.tx.version());
		if (null == pi) throw new RuntimeException("Server routing failure.");
		invoking.parameterClasses = pi.parametersClasses();
	}

	public final Response invoke(final Invoking invoking) {
		Request req = new Request(invoking.tx, invoking.context, invoking.parameters);
		Context.initialize(Context.deserialize(req.context()));
		try {
			return ((StandardBusImpl) invoking.bus).invoke(req, invoking.options);
		} catch (Exception e) {
			e = Exceptions.unwrap(e);
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}