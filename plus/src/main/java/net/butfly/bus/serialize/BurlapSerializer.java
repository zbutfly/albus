package net.butfly.bus.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.apache.http.entity.ContentType;

import com.caucho.burlap.io.BurlapInput;
import com.caucho.burlap.io.BurlapOutput;

public class BurlapSerializer extends HessianSupport {
	@Override
	public void write(OutputStream os, Object obj) throws IOException {
		BurlapOutput ho = new BurlapOutput(os);
		if (null != factory) ho.setSerializerFactory(factory);
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
		BurlapInput hi = new BurlapInput(is);
		if (null != factory) hi.setSerializerFactory(factory);
		try {
			return (T) hi.readObject();
		} finally {
			hi.close();
		}
	}

	@Override
	public void readThenWrite(InputStream is, OutputStream os, Type... types) throws IOException {
		write(os, read(is, types));
	}

	@Override
	public boolean supportHTTPStream() {
		return true;
	}

	@Override
	public ContentType[] getSupportedContentTypes() {
		return new ContentType[] { HessianSupport.BURLAP_CONTENT_TYPE };
	}
}
