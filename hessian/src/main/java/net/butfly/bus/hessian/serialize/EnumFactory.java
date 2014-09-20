package net.butfly.bus.hessian.serialize;

import net.butfly.albacore.support.EnumSupport;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EnumFactory extends AbstractSerializerFactory {
	@Override
	public Deserializer getDeserializer(Class clazz) throws HessianProtocolException {
		if (EnumSupport.class.isAssignableFrom(clazz)) { return new EnumDeserializer(clazz); }
		return null;
	}

	@Override
	public Serializer getSerializer(Class clazz) throws HessianProtocolException {
		if (EnumSupport.class.isAssignableFrom(clazz)) { return EnumSerializer.instance(); }
		return null;
	}
}
