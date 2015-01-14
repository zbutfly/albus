package net.butfly.bus.context;

import net.butfly.albacore.utils.UtilsBase;
import net.butfly.bus.service.InternalFacade;

/**
 * For campability.
 * 
 * @author butfly
 */
@Deprecated
public class Constants extends UtilsBase {
	@Deprecated
	public static interface InternalTX {
		final String PING = InternalFacade.BUS_INTERNAL_TX_PING;
		final String ECHO = InternalFacade.BUS_INTERNAL_TX_ECHO;
		final String SLEEP = InternalFacade.BUS_INTERNAL_TX_SLEEP;
	}
}
