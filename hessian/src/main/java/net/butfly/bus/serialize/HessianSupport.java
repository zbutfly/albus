package net.butfly.bus.serialize;

import java.util.List;

import net.butfly.albacore.exception.SystemException;

//import org.apache.http.Consts;
import org.apache.http.entity.ContentType;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.SerializerFactory;

public abstract class HessianSupport extends HTTPStreamingSupport implements Serializer, SerializerFactorySupport {
	public static final ContentType HESSIAN_CONTENT_TYPE = ContentType.create("x-application/hessian"/*, Consts.ISO_8859_1*/);
	public static final ContentType BURLAP_CONTENT_TYPE = ContentType.create("x-application/burlap"/*, Consts.ISO_8859_1*/);
	protected SerializerFactory factory;

	@Override
	public void addFactoriesByClassName(List<String> classes) {
		if (this.factory == null) this.factory = new SerializerFactory();
		for (String f : classes)
			try {
				this.factory.addFactory((AbstractSerializerFactory) Class.forName(f).newInstance());
			} catch (Exception e) {
				throw new SystemException("", e);
			}
	}
}
