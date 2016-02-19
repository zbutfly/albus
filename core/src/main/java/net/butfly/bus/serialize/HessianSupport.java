package net.butfly.bus.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.apache.http.entity.ContentType;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.SerializerFactory;
import com.google.common.base.Charsets;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.Reflections;

public abstract class HessianSupport extends SerializerBase implements Serializer, SerializerFactorySupport {
	public static final ContentType APPLICATION_HESSIAN = ContentType.create("x-application/hessian", Charsets.UTF_8);
	public static final ContentType APPLICATION_BURLAP = ContentType.create("x-application/burlap", Charsets.UTF_8);

	protected SerializerFactory factory;

	public HessianSupport(Charset charset) {
		super(charset);
	}

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
	public <T> T deserialize(byte[] data, Type type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] deserialize(byte[] data, Type[] types) {
		try {
			return this.read(new ByteArrayInputStream(data), types);
		} catch (IOException ex) {
			throw new SystemException("", ex);
		}
	}

	@Override
	public void addFactoriesByClassName(String... classes) {
		if (this.factory == null) this.factory = new SerializerFactory();
		if (null != classes) for (String f : classes)
			try {
				AbstractSerializerFactory fact = Reflections.construct(f);
				this.factory.addFactory(fact);
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
		return new String(serialize(obj), this.charset());
	}

	@Override
	public <T> T fromString(String str, Type type) {
		return deserialize(str.getBytes(this.charset()), type);
	}

	@Override
	public Object[] fromString(final String str, final Type[] types) {
		return deserialize(str.getBytes(this.charset()), types);
	}
}
