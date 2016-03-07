package net.butfly.bus.config.parser;

<<<<<<< HEAD
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.butfly.bus.config.Configuration;

=======
import net.butfly.bus.config.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
public abstract class Parser {
	protected static Logger logger = LoggerFactory.getLogger(Parser.class);

	public Parser() {}

	public abstract Configuration parse();
}
