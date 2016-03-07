package net.butfly.bus.impl;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.Exceptions;
<<<<<<< HEAD
import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.Bus;
import net.butfly.bus.Mode;
=======
import net.butfly.bus.Bus;
import net.butfly.bus.Bus.Mode;
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
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

<<<<<<< HEAD
	static Bus create(Mode mode, String conf) {
		// XXX: no compilance error, but runtime failure withou impl.
		try {
			return Reflections.construct("net.butfly.bus.impl.BusImpl", mode, conf);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

=======
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
	public static Bus client(String conf) {
		return create(Mode.CLIENT, conf);
	}

	public static Bus server(String conf) {
<<<<<<< HEAD
		return (Bus) create(Mode.SERVER, conf);
=======
		return create(Mode.SERVER, conf);
	}

	static Bus create(Mode mode, String conf) {
		return new BusImpl(mode, conf);
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
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
<<<<<<< HEAD
			Class<? extends Cluster> c = Reflections.forClassName("net.butfly.bus.impl.AsyncCluster");
			if (null == c) c = Cluster.class;
			return Reflections.construct(c, Mode.SERVER, routerClass == null ? new SimpleRouter() : routerClass.newInstance(),
					configs);
=======
			return new Cluster(Mode.SERVER, routerClass == null ? new SimpleRouter() : routerClass.newInstance(), configs);
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
		} catch (Exception e) {
			throw Exceptions.wrap(e);
		}
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
