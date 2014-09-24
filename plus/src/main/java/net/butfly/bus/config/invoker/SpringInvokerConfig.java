package net.butfly.bus.config.invoker;

import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public class SpringInvokerConfig extends InvokerConfigBean {
	private static final long serialVersionUID = -7366819206702674572L;
	private String files;

	public String getFiles() {
		return files;
	}

	public void setFiles(String files) {
		this.files = files;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(files).append("]");;
		return sb.toString();
	}
}
