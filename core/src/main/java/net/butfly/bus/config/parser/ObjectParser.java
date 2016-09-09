package net.butfly.bus.config.parser;

import net.butfly.bus.config.Configuration;

public class ObjectParser extends Parser {
	protected Configuration config;

	@Override
	public Configuration parse() {
		return this.config;
	}
}
