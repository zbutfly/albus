package net.butfly.bus.hessian.serialize;

import java.io.IOException;

import net.butfly.albacore.support.EnumSupport;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Serializer;

public class EnumSerializer implements Serializer {
	private static EnumSerializer INSTANCE = new EnumSerializer();

	public static EnumSerializer instance() {
		return INSTANCE;
	}

	public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
		out.writeInt(((EnumSupport<?>) obj).value());
	}
}
