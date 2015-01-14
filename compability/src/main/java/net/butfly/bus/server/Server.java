package net.butfly.bus.server;

import net.butfly.bus.Bus;
import net.butfly.bus.impl.BusImplWrapper;
import net.butfly.bus.impl.BusMode;

/**
 * For campability.
 * 
 * @author butfly
 */
@Deprecated
public class Server extends BusImplWrapper implements Bus {
	private static final long serialVersionUID = -9131851518146359535L;

	public Server(String conf) {
		super(conf, BusMode.SERVER);
	}
}
