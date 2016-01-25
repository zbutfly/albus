package net.butfly.bus.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.caucho.hessian.io.Hessian2StreamingInput;
import com.caucho.hessian.io.Hessian2StreamingOutput;
import com.caucho.hessian.io.SerializerFactory;

public class HessianSerializer extends HessianSupport {
	public HessianSerializer(Charset charset) {
		super(charset);
	}

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
	public <T> T read(InputStream is, Type... types) throws IOException {
		Hessian2StreamingInput hi = new Hessian2StreamingInput(is);
		if (null != factory) hi.setSerializerFactory(factory);
		try {
			return (T) hi.readObject();
		} finally {
			hi.close();
		}
	}

	@Override
	public void readThenWrite(InputStream is, OutputStream os, Type... types) throws IOException {
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

	private static final String[] SUPPORTED_MIME = new String[] { APPLICATION_HESSIAN.getMimeType() };

	@Override
	public String[] supportedMimeTypes() {
		return SUPPORTED_MIME;
	}
}
