package net.butfly.bus.console.facade;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.TX;
import net.butfly.bus.config.Config;

public interface ConfigFacade extends Facade {
	@TX("BUS_CSL-00-01")
	Config getConfig(String busId, boolean isServer);
}
