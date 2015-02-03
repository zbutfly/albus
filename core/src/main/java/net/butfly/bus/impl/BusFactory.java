package net.butfly.bus.impl;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.Bus;
import net.butfly.bus.Bus.Mode;
import net.butfly.bus.config.Configuration;
import net.butfly.bus.config.loader.ClasspathLoader;
import net.butfly.bus.config.loader.Loader;
import net.butfly.bus.config.parser.XMLParser;
import net.butfly.bus.context.Context;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.utils.Constants;

public final class BusFactory {
	private BusFactory() {}

	public static Bus client() {
		return create(Mode.CLIENT, null);
	}

	public static Bus client(String conf) {
		return create(Mode.CLIENT, conf);
	}

	public static Bus server() {
		return create(Mode.SERVER, null);
	}

	public static Bus server(String conf) {
		return create(Mode.SERVER, conf);
	}

	static Bus create(Mode mode, String conf) {
		return new BusImpl(mode, conf);
	}

	public static Cluster serverCluster(String... config) {
		return cluster(Mode.SERVER, null, config);
	}

	public static Cluster clientCluster(String... config) {
		return cluster(Mode.CLIENT, null, config);
	}

	public static Cluster serverCluster(Class<? extends Router> routerClass, String... config) {
		return cluster(Mode.SERVER, routerClass, config);
	}

	public static Cluster clientCluster(Class<? extends Router> routerClass, String... config) {
		return cluster(Mode.CLIENT, routerClass, config);
	}

	static Cluster cluster(Mode mode, Class<? extends Router> routerClass, String... configs) {
		try {
			return new Cluster(Mode.SERVER, routerClass == null ? new SimpleRouter() : routerClass.newInstance(), configs);
		} catch (Exception e) {
			throw Exceptions.wrap(e);
		}
	}

	static Cluster serverCluster(String routerClassName, String... config) {
		Class<? extends Router> cl = Reflections.forClassName(routerClassName);
		return cluster(Mode.SERVER, cl, config);
	}

	static Cluster clientCluster(String routerClassName, String... config) throws ClassNotFoundException {
		Class<? extends Router> cl = Reflections.forClassName(routerClassName);
		return cluster(Mode.SERVER, cl, config);
	}

	static Configuration createConfiguration(String configLocation, Mode mode) {
		Configuration config = new XMLParser(scanLoader(configLocation).load()).parse();
		if (config.debug()) Context.debug(true);
		return config;
	}

	static Router createRouter(Configuration config) {
		try {
			return config.getRouter().getRouterClass().newInstance();
		} catch (Throwable e) {
			return new SimpleRouter();
		}
	}

	private static Loader scanLoader(String configLocation) {
		Loader l = new ClasspathLoader(configLocation);
		if (l.load() != null) return l;
		// load default
		l = new ClasspathLoader(Constants.Configuration.DEFAULT_COMMON_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathLoader(Constants.Configuration.DEFAULT_SERVER_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathLoader(Constants.Configuration.DEFAULT_CLIENT_CONFIG);
		if (l.load() != null) return l;
		// internal config
		l = new ClasspathLoader(Constants.Configuration.INTERNAL_COMMON_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathLoader(Constants.Configuration.INTERNAL_SERVER_CONFIG);
		if (l.load() != null) return l;
		l = new ClasspathLoader(Constants.Configuration.INTERNAL_CLIENT_CONFIG);
		if (l.load() != null) return l;
		throw new SystemException(Constants.UserError.CONFIG_ERROR, "StandardBus configurations invalid: " + configLocation);
	}
}
