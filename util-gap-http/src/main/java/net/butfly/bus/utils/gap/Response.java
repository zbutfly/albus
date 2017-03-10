package net.butfly.bus.utils.gap;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map.Entry;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.ServletCookieAdaptor;
import io.undertow.util.HttpString;

/**
 * Response from Ning to Undertow
 * 
 * @author butfly
 */
final class Response extends R {
	private static final long serialVersionUID = -3206517732701548545L;
	final int status;

	public Response(com.ning.http.client.Response resp) {
		status = resp.getStatusCode();
	}

	void response(HttpServerExchange exch) {
		exch.setStatusCode(status);
		exch.setResponseContentLength(body.length);
		for (javax.servlet.http.Cookie c : cookies)
			exch.setResponseCookie(new ServletCookieAdaptor(c));
		for (Entry<String, String[]> h : headers.entrySet())
			exch.getResponseHeaders().addAll(new HttpString(h.getKey()), Arrays.asList(h.getValue()));
		exch.getResponseSender().send(ByteBuffer.wrap(body));
	}

	public long writeTo(OutputStream out) {
		return 0;
		// TODO Auto-generated method stub

	}

	public static Response readFrom(InputStream in) {
		// TODO Auto-generated method stub
		return null;
	}
}
