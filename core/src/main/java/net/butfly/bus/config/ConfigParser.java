package net.butfly.bus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigParser {
	protected static Logger logger = LoggerFactory.getLogger(ConfigParser.class);

	public ConfigParser() {}

	public abstract Config parse();

	@SuppressWarnings("unchecked")
	protected <T> Class<T> classForName(String className) {
		try {
			return (Class<T>) Class.forName(className);
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> T instanceForName(String className) {
		try {
			return (T) classForName(className).newInstance();
		} catch (Exception e) {
			return null;
		}
	}
}
