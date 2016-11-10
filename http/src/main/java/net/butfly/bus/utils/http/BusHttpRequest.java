package net.butfly.bus.utils.http;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.entity.ContentType;

import net.butfly.albacore.utils.logger.Logger;

public class BusHttpRequest {
	String url;
	Map<String, String> headers;
	byte[] data;
	String mimeType;
	Charset charset;

	int timeout;

	public BusHttpRequest(String path, Map<String, String> headers, byte[] data, ContentType contentType, int timeout) {
		this(path, headers, data, contentType.getMimeType(), contentType.getCharset(), timeout);
	}

	public BusHttpRequest(String url, Map<String, String> headers, byte[] data, String mimeType, Charset charset, int timeout) {
		super();
		this.url = url;
		this.headers = headers;
		this.data = data;
		this.mimeType = mimeType;
		this.charset = charset;
		this.timeout = timeout;
	}

	void logRequest(Logger logger) {
		if (logger.isTraceEnabled()) {
			logger.trace("HTTP Request SEND ==> " + url);
			logger.trace("HTTP Request SEND ==> HEADER: " + headers.toString());
			logger.trace("HTTP Request SEND ==> CONTENT[" + data.length + "]: " + new String(data, charset));
		}
	}
}
