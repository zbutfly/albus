package net.butfly.bus.config.parser;

import net.butfly.bus.config.Config;
import net.butfly.bus.config.ConfigParser;

public class ObjectConfigParser extends ConfigParser {
	protected Config config;

	@Override
	public Config parse() {
		return this.config;
	}
}
