package net.butfly.bus.config.bean.invoker;

import net.butfly.bus.config.bean.ConfigBean;

public abstract class InvokerConfigBean extends ConfigBean {
	private static final long serialVersionUID = -1L;

	public abstract String toString();

	public boolean isLazy() {
		return false;
	}
}
