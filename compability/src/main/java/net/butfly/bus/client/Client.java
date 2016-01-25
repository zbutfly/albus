package net.butfly.bus.client;

import net.butfly.bus.StandardBus;
import net.butfly.bus.impl.ClientWrapper;

/**
 * For campability.
 * 
 * @author butfly
 */
@Deprecated
public class Client extends ClientWrapper implements StandardBus {
	private static final long serialVersionUID = 5236932158963485814L;

	public Client(String conf) {
		super(conf);
	}
}
