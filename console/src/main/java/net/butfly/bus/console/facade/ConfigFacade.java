package net.butfly.bus.console.facade;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.TX;

public interface ConfigFacade extends Facade {
	@TX("BUS_CSL-00-01")
	void getConfig(String busId, boolean isServer);
}
