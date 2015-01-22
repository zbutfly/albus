package net.butfly.bus.impl;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.Exceptions;
import net.butfly.bus.config.Config;
import net.butfly.bus.config.ConfigLoader;
import net.butfly.bus.config.loader.ClasspathConfigLoad;
import net.butfly.bus.config.parser.XMLConfigParser;
import net.butfly.bus.context.Context;
import net.butfly.bus.impl.BusFactory.Mode;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.utils.Constants;

class BusFactoryImpl {
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

	static Cluster cluster(String[] config, Class<? extends Router> routerClass, Mode mode) {
		try {
			return new Cluster(config, Mode.SERVER, routerClass == null ? new SimpleRouter() : routerClass.newInstance());
		} catch (Exception e) {
			throw Exceptions.wrap(e);
		}

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
