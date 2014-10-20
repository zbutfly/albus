package net.butfly.bus.config.invoker;

import java.util.List;

import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

import com.caucho.hessian.io.AbstractSerializerFactory;

public class HessianInvokerConfig extends InvokerConfigBean {
	private static final long serialVersionUID = -7791541622206850647L;
	private String path;
	private long timeout;
	private List<Class<? extends AbstractSerializerFactory>> typeTranslators;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<Class<? extends AbstractSerializerFactory>> getTypeTranslators() {
		return typeTranslators;
	}

	public void setTypeTranslators(List<Class<? extends AbstractSerializerFactory>> typeTranslators) {
		this.typeTranslators = typeTranslators;
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
		for (Class<? extends AbstractSerializerFactory> clazz : typeTranslators)
			sb.append(":").append(clazz.getName());
		sb.append("]");
		return sb.toString();
	}

}
