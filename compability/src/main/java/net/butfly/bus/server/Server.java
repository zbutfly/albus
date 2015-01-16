package net.butfly.bus.server;

import net.butfly.bus.StandardBus;
import net.butfly.bus.impl.ServerWrapper;

/**
 * For campability.
 * 
 * @author butfly
 */
@Deprecated
public class Server extends ServerWrapper implements StandardBus {
	private static final long serialVersionUID = -9131851518146359535L;

	public Server(String conf) {
		super(conf);
	}
}
