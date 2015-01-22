package net.butfly.bus.config.invoker;

import java.util.List;

import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.serialize.JSONSerializer;

public class WebServiceInvokerConfig extends InvokerConfigBean {
	private static final long serialVersionUID = -7791541622206850647L;
	private String path;
	private int timeout;
	private List<String> typeTranslators;
	private String serializer;

	public List<String> getTypeTranslators() {
		return typeTranslators;
	}

	public void setTypeTranslators(List<String> typeTranslators) {
		this.typeTranslators = typeTranslators;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(path).append(":").append(timeout);
		if (typeTranslators != null) for (String clazz : typeTranslators)
			sb.append(":").append(clazz);
		return sb.toString();
	}

	public String getSerializer() {
		return this.serializer == null ? JSONSerializer.class.getName() : this.serializer;
	}

	public void setSerializer(String serializerClassname) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		this.serializer = serializerClassname;
	}
}
