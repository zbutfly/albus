package net.butfly.bus.console.facade;

import net.butfly.albacore.facade.FacadeBase;
import net.butfly.bus.config.Config;
import net.butfly.bus.console.service.ClientConfigService;
import net.butfly.bus.console.service.ServerConfigService;

public class ConfigFacadeImpl extends FacadeBase implements ConfigFacade {
	private static final long serialVersionUID = -8333878083232698633L;
	private ClientConfigService clientConfigService;
	private ServerConfigService serverConfigService;

	public void setClientConfigService(ClientConfigService clientConfigService) {
		this.clientConfigService = clientConfigService;
	}

	public void setServerConfigService(ServerConfigService serverConfigService) {
		this.serverConfigService = serverConfigService;
	}

	@Override
	public Config getConfig(String busId, boolean isServer) {
		// TODO
		if (isServer && null == this.serverConfigService) return null;
		if (null == this.clientConfigService) return null;
		return null;
	}
}
