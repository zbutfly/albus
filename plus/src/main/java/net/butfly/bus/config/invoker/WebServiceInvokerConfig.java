package net.butfly.bus.config.invoker;

import java.util.List;

import com.caucho.hessian.io.AbstractSerializerFactory;

import net.butfly.albacore.utils.serialize.HessianSerializer;
import net.butfly.albacore.utils.serialize.Serializer;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public class WebServiceInvokerConfig extends InvokerConfigBean {
	private static final long serialVersionUID = -7791541622206850647L;
	private String path;
	private long timeout;
	private List<Class<? extends AbstractSerializerFactory>> typeTranslators;

	public List<Class<? extends AbstractSerializerFactory>> getTypeTranslators() {
		return typeTranslators;
	}

	public void setTypeTranslators(List<Class<? extends AbstractSerializerFactory>> typeTranslators) {
		this.typeTranslators = typeTranslators;
	}

	private Class<? extends Serializer> serializer;

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
		for (Class<? extends AbstractSerializerFactory> clazz : typeTranslators)
			sb.append(":").append(clazz.getName());
		sb.append("]");
		return sb.toString();
	}

	public Class<? extends Serializer> getSerializer() {
		return this.serializer;
	}

	@SuppressWarnings("unchecked")
	public void setSerializer(String serializerClassname) {
		try {
			this.serializer = (Class<? extends Serializer>) Thread.currentThread().getContextClassLoader()
					.loadClass(serializerClassname);
		} catch (Exception e) {
			this.serializer = HessianSerializer.class;
		}
	}
}
