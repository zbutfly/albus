package net.butfly.bus.hessian.serialize;

import java.io.IOException;
import java.util.ArrayList;

import net.butfly.albacore.support.EnumSupport;
import net.butfly.albacore.utils.EnumUtils;

import com.caucho.hessian.io.AbstractDeserializer;
import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.Deserializer;

public class EnumDeserializer extends AbstractDeserializer implements Deserializer {
	private Class<EnumSupport<?>> clazz;

	public EnumDeserializer(Class<EnumSupport<?>> clazz) {
		// hessian/33b[34], hessian/3bb[78]
		this.clazz = clazz;
	}

	public Class<?> getType() {
		return this.clazz;
	}

	public Object readObject(AbstractHessianInput in) throws IOException {
		return this.readEnum(in);
	}

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

	public Object readLengthList(AbstractHessianInput in, int length) throws IOException {
		EnumSupport<?>[] data = new EnumSupport[length];
		in.addRef(data);
		for (int i = 0; i < data.length; i++) {
			data[i] = this.readEnum(in);
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	private <E extends EnumSupport<?>> E readEnum(AbstractHessianInput in) throws IOException {
		return (E) EnumUtils.valueOf(this.clazz, in.readInt());
	}
}
