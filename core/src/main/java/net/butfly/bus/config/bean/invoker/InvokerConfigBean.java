package net.butfly.bus.config.bean.invoker;

import net.butfly.bus.config.bean.ConfigBean;

public abstract class InvokerConfigBean extends ConfigBean {
	private static final long serialVersionUID = -1L;
	private String continuousSupported = null;

	public abstract String toString();

	public String getContinuousSupported() {
		return continuousSupported;
	}

	public void setContinuousSupported(String continuousSupported) {
		this.continuousSupported = continuousSupported;
	}
}
