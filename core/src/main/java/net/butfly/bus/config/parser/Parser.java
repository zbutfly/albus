package net.butfly.bus.config.parser;

import net.butfly.albacore.utils.logger.Logger;

import net.butfly.bus.config.Configuration;

public abstract class Parser {
	protected static Logger logger = Logger.getLogger(Parser.class);

	public Parser() {}

	public abstract Configuration parse();
}
