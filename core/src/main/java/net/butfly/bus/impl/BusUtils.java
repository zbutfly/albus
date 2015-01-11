package net.butfly.bus.impl;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.UtilsBase;
import net.butfly.bus.config.Config;
import net.butfly.bus.config.ConfigLoader;
import net.butfly.bus.config.loader.ClasspathConfigLoad;
import net.butfly.bus.config.parser.XMLConfigParser;
import net.butfly.bus.context.Context;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.utils.Constants;

final class BusUtils extends UtilsBase {
	static Config createConfiguration(String configLocation, BusMode mode) {
		Config config = new XMLConfigParser(scanLoader(configLocation).load()).parse();
		Context.initialize(null);
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
		throw new SystemException(Constants.UserError.CONFIG_ERROR, "Bus configurations invalid: " + configLocation);
	}
}
