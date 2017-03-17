package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
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
		for (com.ning.http.client.cookie.Cookie c : resp.getCookies())
			cookies.add(HttpClient.unconv(c));
		for (Entry<String, List<String>> h : resp.getHeaders())
			headers.put(h.getKey(), h.getValue());
		try {
			body = resp.getResponseBodyAsBytes();
		} catch (IOException e) {
			body = new byte[0];
		}
	}

	public boolean response(HttpServerExchange exch) {
		exch.setStatusCode(status);
		for (javax.servlet.http.Cookie c : cookies)
			exch.setResponseCookie(new ServletCookieAdaptor(c));
		for (Entry<String, Collection<String>> h : headers.entrySet())
			exch.getResponseHeaders().addAll(new HttpString(h.getKey()), h.getValue());
		exch.getResponseSender().send(ByteBuffer.wrap(null == body ? new byte[0] : body));
		return true;
	}

	@Override
	public HttpResponse save(OutputStream out) {
		try {
			IOs.writeInt(out, status);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return super.save(out);
	}

	@Override
	public HttpResponse load(InputStream in) {
		try {
			status = IOs.readInt(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return super.load(in);
	}
}
