package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.entity.ContentType;

public abstract class HttpHandler {
	protected int connTimeout;
	protected int readTimeout;

	public HttpHandler(int connTimeout, int readTimeout) {
		this.connTimeout = connTimeout >= 0 ? connTimeout : 0;
		this.readTimeout = readTimeout >= 0 ? readTimeout : 0;
	}

	public abstract InputStream post(String url, byte[] data, ContentType contentType, Map<String, String> headers,
			boolean streaming) throws IOException;
}
