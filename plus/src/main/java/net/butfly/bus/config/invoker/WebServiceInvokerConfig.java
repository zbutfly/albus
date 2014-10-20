package net.butfly.bus.config.invoker;

import java.util.List;

import net.butfly.albacore.utils.serialize.JSONSerializer;
import net.butfly.albacore.utils.serialize.Serializer;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

import com.caucho.hessian.io.AbstractSerializerFactory;

public class WebServiceInvokerConfig extends InvokerConfigBean {
	private static final long serialVersionUID = -7791541622206850647L;
	private String path;
	private int timeout;
	private List<Class<? extends AbstractSerializerFactory>> typeTranslators;
	private Serializer serializer;

	public List<Class<? extends AbstractSerializerFactory>> getTypeTranslators() {
		return typeTranslators;
	}

	public void setTypeTranslators(List<Class<? extends AbstractSerializerFactory>> typeTranslators) {
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
		sb.append("[").append(path).append(":").append(timeout);
		for (Class<? extends AbstractSerializerFactory> clazz : typeTranslators)
			sb.append(":").append(clazz.getName());
		sb.append("]");
		return sb.toString();
	}

	public Serializer getSerializer() {
		return this.serializer == null ? new JSONSerializer() : this.serializer;
	}

	public void setSerializer(String serializerClassname) throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		this.serializer = (Serializer) Class.forName(serializerClassname).newInstance();
	}
}
