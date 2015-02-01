package net.butfly.bus.config.parser;

import net.butfly.bus.config.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Parser {
	protected static Logger logger = LoggerFactory.getLogger(Parser.class);

	public Parser() {}

	public abstract Configuration parse();
}