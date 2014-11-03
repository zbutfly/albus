package net.butfly.albacore.utils.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.Hessian2StreamingInput;
import com.caucho.hessian.io.Hessian2StreamingOutput;
import com.caucho.hessian.io.SerializerFactory;

public class HessianSerializer extends HTTPStreamingSupport implements Serializer, SerializerFactorySupport {
	private SerializerFactory factory;

	@Override
	public void write(OutputStream os, Object obj) throws IOException {
		Hessian2StreamingOutput ho = new Hessian2StreamingOutput(os);
		if (null != factory) ho.getHessian2Output().setSerializerFactory(factory);
		ho.setCloseStreamOnClose(false);
		try {
			ho.writeObject(obj);
		} finally {
			ho.close();
		}
		os.flush();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T read(InputStream is, Class<?>... types) throws IOException {
		Hessian2StreamingInput hi = new Hessian2StreamingInput(is);
		if (null != factory) hi.setSerializerFactory(factory);
		try {
			return (T) hi.readObject();
		} finally {
			hi.close();
		}
	}

	@Override
	public void readThenWrite(InputStream is, OutputStream os, Class<?>... types) throws IOException {
		Hessian2StreamingInput hi = new Hessian2StreamingInput(is);
		if (null != factory) hi.setSerializerFactory(factory);
		try {
			hi.startPacket().readToOutputStream(os);
			hi.endPacket();
		} finally {
			hi.close();
		}
	}

	public void setFactory(SerializerFactory factory) {
		this.factory = factory;
	}

	@Override
	public boolean supportHTTPStream() {
		return true;
	}

	@Override
	public String[] getContentTypes() {
		return new String[] { "x-application/hessian" };
	}

	@Override
	public void addFactory(AbstractSerializerFactory factory) {
		this.factory.addFactory(factory);
	}
}
