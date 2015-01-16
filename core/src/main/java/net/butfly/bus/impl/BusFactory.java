package net.butfly.bus.impl;

import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.ExceptionUtils;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.config.Config;
import net.butfly.bus.config.ConfigLoader;
import net.butfly.bus.config.bean.invoker.InvokerBean;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.config.loader.ClasspathConfigLoad;
import net.butfly.bus.config.parser.XMLConfigParser;
import net.butfly.bus.context.Context;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.utils.Constants;

public final class BusFactory {
	private BusFactory() {}

	enum Mode {
		SERVER, CLIENT;
	}

	static CallbackBus create(String conf, Mode mode) {
		return new CallbackBusImpl(conf, mode);
	}

	public static CallbackBus client() {
		return create(null, Mode.CLIENT);
	}

	public static CallbackBus client(String conf) {
		return create(conf, Mode.CLIENT);
	}

	public static CallbackBus server() {
		return create(null, Mode.SERVER);
	}

	public static CallbackBus server(String conf) {
		return create(conf, Mode.SERVER);
	}

	static Cluster cluster(String[] config, Class<? extends Router> routerClass, Mode mode) {
		try {
			return new Cluster(config, Mode.SERVER, routerClass == null ? new SimpleRouter() : routerClass.newInstance());
		} catch (Exception e) {
			throw ExceptionUtils.wrap(e);
		}

	}

	public static Cluster serverCluster(String[] config) {
		return cluster(config, null, Mode.SERVER);
	}

	public static Cluster serverCluster(String[] config, Class<? extends Router> routerClass) {
		return cluster(config, routerClass, Mode.SERVER);
	}

	@SuppressWarnings("unchecked")
	static Cluster serverCluster(String[] config, String routerClassName) throws ClassNotFoundException {
		return cluster(config, null == routerClassName ? null : (Class<? extends Router>) Class.forName(routerClassName),
				Mode.SERVER);
	}

	public static Cluster clientCluster(String[] config) {
		return cluster(config, null, Mode.CLIENT);
	}

	public static Cluster clientCluster(String[] config, Class<? extends Router> routerClass) {
		return cluster(config, routerClass, Mode.CLIENT);
	}

	@SuppressWarnings("unchecked")
	static Cluster clientCluster(String[] config, String routerClassName) throws ClassNotFoundException {
		return cluster(config, null == routerClassName ? null : (Class<? extends Router>) Class.forName(routerClassName),
				Mode.SERVER);
	}

	static Config createConfiguration(String configLocation, Mode mode) {
		Config config = new XMLConfigParser(scanLoader(configLocation).load()).parse();
		Context.initialize(null);
		if (config.debug()) Context.debug(true);
		return config;
	}

	static Router createRouter(Config config) {
		try {
			return config.getRouter().getRouterClass().newInstance();
		} catch (Throwable e) {
			return new SimpleRouter();
		}
	}

	private static Map<String, Invoker<?>> INVOKER_POOL = new HashMap<String, Invoker<?>>();

	@SuppressWarnings("unchecked")
	static <C extends InvokerConfigBean> Invoker<C> getInvoker(InvokerBean bean, Mode mode) {
		Class<? extends Invoker<C>> clazz = (Class<? extends Invoker<C>>) bean.type();
		C config = (C) bean.config();
		String key = bean.id();
		if (null != config) key = key + ":" + config.toString();
		if (INVOKER_POOL.containsKey(key)) return (Invoker<C>) INVOKER_POOL.get(key);
		try {
			Invoker<C> invoker = clazz.newInstance();
			invoker.initialize(config, bean.getToken());
			INVOKER_POOL.put(key, invoker);
			return invoker;
		} catch (Throwable e) {
			throw new SystemException(Constants.UserError.NODE_CONFIG, "Invoker " + clazz.getName()
					+ " initialization failed for invalid Invoker instance.", e);
		}
	}

	private static ConfigLoader scanLoader(String configLocation) {
		ConfigLoader l = new ClasspathConfigLoad(configLocation);
		if (l.load() != null) return l;
		// load default
		l = new ClasspathConfigLoad(Constants.Configuration.DEFAULT_COMMON_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathConfigLoad(Constants.Configuration.DEFAULT_SERVER_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathConfigLoad(Constants.Configuration.DEFAULT_CLIENT_CONFIG);
		if (l.load() != null) return l;
		// internal config
		l = new ClasspathConfigLoad(Constants.Configuration.INTERNAL_COMMON_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathConfigLoad(Constants.Configuration.INTERNAL_SERVER_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathConfigLoad(Constants.Configuration.INTERNAL_CLIENT_CONFIG);
		if (l.load() != null) return l;
		throw new SystemException(Constants.UserError.CONFIG_ERROR, "StandardBus configurations invalid: " + configLocation);
	}
}
