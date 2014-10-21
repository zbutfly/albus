package net.butfly.bus.facade;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.argument.TX;

public interface InternalFacade extends Facade {
	public static final String BUS_INTERNAL_TX_PREFIX = "BUS_ITN-";
	public static final String BUS_INTERNAL_TX_PING = BUS_INTERNAL_TX_PREFIX + "01";
	public static final String BUS_INTERNAL_TX_ECHO = BUS_INTERNAL_TX_PREFIX + "01";
	public static final String BUS_INTERNAL_TX_SLEEP = BUS_INTERNAL_TX_PREFIX + "01";

	@TX(BUS_INTERNAL_TX_PING)
	long ping();

	@TX(BUS_INTERNAL_TX_ECHO)
	String echo(String echo);

	@TX(BUS_INTERNAL_TX_SLEEP)
	void sleep(long ms);
}
