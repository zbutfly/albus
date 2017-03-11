package net.butfly.bus.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import com.ning.http.client.ListenableFuture;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;

/**
 * Request from Undertow to Ning
 * 
 * @author butfly
 */
public final class Request extends HttpWrapper {
	private static final long serialVersionUID = 8927918437569361626L;
	protected String method;
	protected String url;

	public Request(HttpServerExchange exch) {
		method = exch.getRequestMethod().toString();
		url = exch.getRequestURL();
		headers = new HashMap<>();
		for (HeaderValues h : exch.getRequestHeaders())
			headers.put(h.getHeaderName().toString(), h.toArray());
		cookies = new ArrayList<>();
		for (io.undertow.server.handlers.Cookie c : exch.getRequestCookies().values())
			cookies.add(new javax.servlet.http.Cookie(c.getName(), c.getValue()));
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		exch.getRequestReceiver().receiveFullBytes((ex, bytes) -> {
			try {
				os.write(bytes);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		body = os.toByteArray();
	}

	public void request(HttpClient client, Consumer<com.ning.http.client.Response> using) {
		ListenableFuture<com.ning.http.client.Response> f = client.requestBuilder(method, url).execute();
		f.addListener(() -> {
			com.ning.http.client.Response r;
			try {
				r = f.get();
			} catch (InterruptedException e) {
				return;
			} catch (ExecutionException e) {
				return;
			}
			using.accept(r);
		}, ForkJoinPool.commonPool());
	}

	public static Request readFrom(InputStream data) {
		// TODO Auto-generated method stub
		return null;
	}

	public long writeTo(OutputStream os) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Request redirect(String host, int port) {
		// TODO Auto-generated method stub
		return this;
	}
}
