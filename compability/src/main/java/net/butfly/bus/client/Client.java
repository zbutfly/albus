package net.butfly.bus.client;

import net.butfly.bus.StandardBus;
import net.butfly.bus.impl.BusImplWrapper;
import net.butfly.bus.impl.BusMode;

/**
 * For campability.
 * 
 * @author butfly
 */
@Deprecated
public class Client extends BusImplWrapper implements StandardBus {
	private static final long serialVersionUID = 5236932158963485814L;

	public Client(String conf) {
		super(conf, BusMode.CLIENT);
	}
}
