package net.butfly.bus.config.invoker;

import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public class WebServiceInvokerConfig extends InvokerConfigBean {
	private static final long serialVersionUID = -7791541622206850647L;
	private String path;
	private long timeout;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(path).append(":").append(timeout);
		sb.append("]");
		return sb.toString();
	}
}
