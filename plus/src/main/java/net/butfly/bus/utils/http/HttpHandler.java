package net.butfly.bus.utils.http;

import java.io.IOException;
import java.util.Map;

import net.butfly.bus.invoker.WebServiceInvoker.HandlerResponse;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HttpHandler {
	private static Logger logger = LoggerFactory.getLogger(HttpHandler.class);
	protected int connTimeout;
	protected int readTimeout;

	

	public HttpHandler(int connTimeout, int readTimeout) {
		this.connTimeout = connTimeout >= 0 ? connTimeout : 0;
		this.readTimeout = readTimeout >= 0 ? readTimeout : 0;
	}

	public abstract HandlerResponse post(String url, Map<String, String> headers, byte[] data, ContentType contentType,
			boolean streaming) throws IOException;

	protected void logRequest(String url, Map<String, String> headers, String data, boolean streaming) {
		logger.trace("HTTP Request SEND ==> to: " + url + ", with" + (streaming ? "" : "out") + " streaming.");
		logger.trace("HTTP Request SEND ==> HEADER: " + headers.toString());
		logger.trace("HTTP Request SEND ==> CONTENT: " + data);
	}
}
