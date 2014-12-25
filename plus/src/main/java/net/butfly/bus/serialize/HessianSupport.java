package net.butfly.bus.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import net.butfly.albacore.exception.SystemException;

//import org.apache.http.Consts;
import org.apache.http.entity.ContentType;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.SerializerFactory;

public abstract class HessianSupport extends HTTPStreamingSupport implements Serializer, SerializerFactorySupport {
	// , Consts . ISO_8859_1
	public static final ContentType HESSIAN_CONTENT_TYPE = ContentType.create("x-application/hessian");
	public static final ContentType BURLAP_CONTENT_TYPE = ContentType.create("x-application/burlap");
	protected SerializerFactory factory;

	@Override
	public byte[] serialize(Object obj) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			this.write(os, obj);
		} catch (IOException ex) {
			throw new SystemException("", ex);
		}
		return os.toByteArray();
	}

	@Override
	public <T> T deserialize(byte[] data, Type... types) {
		try {
			return this.read(new ByteArrayInputStream(data), types);
		} catch (IOException ex) {
			throw new SystemException("", ex);
		}
	}

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

	@Override
	public boolean supportClass() {
		return true;
	}

	@Override
	public String asString(Object obj) {
		return new String(serialize(obj), this.getOutputContentType().getCharset());
	}

	@Override
	public <T> T fromString(String str, Type... types) {
		return deserialize(str.getBytes(this.getOutputContentType().getCharset()), types);
	}
}
