package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map.Entry;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.ServletCookieAdaptor;
import io.undertow.util.HttpString;
import net.butfly.albacore.utils.IOs;

/**
 * Response from Ning to Undertow
 * 
 * @author butfly
 */
public final class HttpResponse extends HttpWrapper<HttpResponse> {
	private static final long serialVersionUID = -3206517732701548545L;
	int status;

	public HttpResponse() {
		super();
	}

	public HttpResponse(com.ning.http.client.Response resp) {
		super();
		status = resp.getStatusCode();
	}

	public void response(HttpServerExchange exch) {
		exch.setStatusCode(status);
		exch.setResponseContentLength(body.length);
		for (javax.servlet.http.Cookie c : cookies)
			exch.setResponseCookie(new ServletCookieAdaptor(c));
		for (Entry<String, String[]> h : headers.entrySet())
			exch.getResponseHeaders().addAll(new HttpString(h.getKey()), Arrays.asList(h.getValue()));
		exch.getResponseSender().send(ByteBuffer.wrap(body));
	}

	@Override
	public HttpResponse save(OutputStream out) throws IOException {
		IOs.writeInt(out, status);
		return super.save(out);
	}

	@Override
	public HttpResponse load(InputStream in) throws IOException {
		status = IOs.readInt(in);
		return super.load(in);
	}
}
