package net.butfly.bus.serialize.converter;

import java.io.IOException;
import java.util.ArrayList;

import net.butfly.albacore.support.EnumSupport;
import net.butfly.albacore.utils.EnumUtils;

import com.caucho.hessian.io.AbstractDeserializer;
import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class HessianEnumFactory extends AbstractSerializerFactory {
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

	public static class EnumSerializer implements Serializer {
		private static EnumSerializer INSTANCE = new EnumSerializer();

		public static EnumSerializer instance() {
			return INSTANCE;
		}

		@Override
		public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
			out.writeInt(((EnumSupport<?>) obj).value());
		}
	}

	public class EnumDeserializer extends AbstractDeserializer implements Deserializer {
		private Class<EnumSupport<?>> clazz;

		public EnumDeserializer(Class<EnumSupport<?>> clazz) {
			// hessian/33b[34], hessian/3bb[78]
			this.clazz = clazz;
		}

		@Override
		public Class<?> getType() {
			return this.clazz;
		}

		@Override
		public Object readObject(AbstractHessianInput in) throws IOException {
			return this.readEnum(in);
		}

		@Override
		public Object readList(AbstractHessianInput in, int length) throws IOException {
			if (length >= 0) {
				EnumSupport<?>[] data = new EnumSupport[length];
				in.addRef(data);
				for (int i = 0; i < data.length; i++) {
					data[i] = this.readEnum(in);
				}
				in.readEnd();
				return data;
			} else {
				ArrayList<EnumSupport<?>> list = new ArrayList<EnumSupport<?>>();
				while (!in.isEnd()) {
					list.add(this.readEnum(in));
				}
				in.readEnd();
				EnumSupport<?>[] data = (EnumSupport[]) list.toArray(new EnumSupport[list.size()]);
				in.addRef(data);
				return data;
			}
		}

		@Override
		public Object readLengthList(AbstractHessianInput in, int length) throws IOException {
			EnumSupport<?>[] data = new EnumSupport[length];
			in.addRef(data);
			for (int i = 0; i < data.length; i++) {
				data[i] = this.readEnum(in);
			}
			return data;
		}

		private <E extends EnumSupport<?>> E readEnum(AbstractHessianInput in) throws IOException {
			return (E) EnumUtils.valueOf(this.clazz, in.readInt());
		}
	}
}
