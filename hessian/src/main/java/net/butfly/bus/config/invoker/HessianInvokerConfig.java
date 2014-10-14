package net.butfly.bus.config.invoker;

import java.util.List;

import com.caucho.hessian.io.AbstractSerializerFactory;

public class HessianInvokerConfig extends WebServiceInvokerConfig {
	private static final long serialVersionUID = -7791541622206850647L;
	private List<Class<? extends AbstractSerializerFactory>> typeTranslators;

	public List<Class<? extends AbstractSerializerFactory>> getTypeTranslators() {
		return typeTranslators;
	}

	public void setTypeTranslators(List<Class<? extends AbstractSerializerFactory>> typeTranslators) {
		this.typeTranslators = typeTranslators;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString()).append("[type stanslators");
		for (Class<? extends AbstractSerializerFactory> clazz : typeTranslators)
			sb.append(":").append(clazz.getName());
		sb.append("]");
		return sb.toString();
	}
}
