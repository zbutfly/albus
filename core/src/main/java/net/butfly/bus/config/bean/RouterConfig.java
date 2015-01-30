package net.butfly.bus.config.bean;

import net.butfly.bus.policy.Router;

public class RouterConfig extends Config {
	private static final long serialVersionUID = -8347256263588505080L;
	private Class<? extends Router> type;

	public RouterConfig(Class<? extends Router> type) {
		super();
		this.type = type;
	}

	public Class<? extends Router> getRouterClass() {
		return type;
	}
}
