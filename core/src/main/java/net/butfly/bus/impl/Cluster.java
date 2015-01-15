package net.butfly.bus.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.utils.ExceptionUtils;
import net.butfly.albacore.utils.ReflectionUtils.MethodInfo;
import net.butfly.bus.Bus;
import net.butfly.bus.Bus.Mode;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;
import net.butfly.bus.invoker.Invoking;
import net.butfly.bus.policy.Routeable;
import net.butfly.bus.policy.Router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Cluster implements Routeable {
	private static Logger logger = LoggerFactory.getLogger(Cluster.class);
	private final Map<String, Bus> nodes = new HashMap<String, Bus>();
	final private Mode mode;
	final private Router router;

	public Cluster(String[] configs, Class<? extends Router> clusterRouterClass, boolean supportCallback) {
		this(configs, Mode.CLIENT, clusterRouterClass, supportCallback);
	}

	public Cluster(String[] config, Mode mode, Class<? extends Router> clusterRouterClass, boolean supportCallback) {
		this.mode = mode;
		Class<? extends Bus> busClass = supportCallback ? CallbackBusImpl.class : StandardBusImpl.class;
		try {
			this.router = clusterRouterClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (config == null || config.length == 0) this.registerSingle(null, busClass);
		else for (String conf : config)
			if (!"".equals(conf.trim())) this.registerSingle(conf, busClass);
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

	private void registerSingle(String conf, Class<? extends Bus> busClass) {
		Bus impl = (Bus) BusFactory.bus(busClass, conf, mode);
		nodes.put(impl.id(), impl);
	}

	public void invoking(Invoking invoking) {
		invoking.bus = router.route(invoking.tx.value(), servers());
		if (null == invoking.bus) throw new RuntimeException("Server routing failure.");
		MethodInfo pi = ((BusBase) invoking.bus).invokeInfo(invoking.tx.value(), invoking.tx.version());
		if (null == pi) throw new RuntimeException("Server routing failure.");
		invoking.parameterClasses = pi.parametersClasses();
	}

	public final Response invoke(final Invoking invoking) {
		Request req = new Request(invoking.tx, invoking.context, invoking.parameters);
		Context.initialize(Context.deserialize(req.context()));
		try {
			return ((StandardBusImpl) invoking.bus).invoke(req, invoking.options);
		} catch (Exception e) {
			e = ExceptionUtils.unwrap(e);
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}
