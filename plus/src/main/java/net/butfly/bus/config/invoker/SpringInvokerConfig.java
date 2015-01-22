package net.butfly.bus.config.invoker;

import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public class SpringInvokerConfig extends InvokerConfigBean {
	private static final long serialVersionUID = -7366819206702674572L;
	private String files;
	private String lazy = "false";

	public String getFiles() {
		return files;
	}

	public void setFiles(String files) {
		this.files = files;
	}

	@Override
	public boolean isLazy() {
		String l = System.getProperty("bus.invoker.spring.lazy");
		return Boolean.parseBoolean(l == null ? lazy : l);
	}

	@Override
	public String toString() {
		return files;
	}
}
