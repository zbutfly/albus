package net.butfly.bus.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ning.http.client.ListenableFuture;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import net.butfly.albacore.utils.IOs;
import net.butfly.albacore.utils.logger.Logger;

/**
 * Request from Undertow to Ning
 * 
 * @author butfly
 */
public final class HttpRequest extends HttpWrapper<HttpRequest> {
	private static final long serialVersionUID = 8927918437569361626L;
	private static final transient Logger logger = Logger.getLogger(HttpRequest.class);

	protected String method;
	protected String url;

	public HttpRequest(HttpServerExchange exch) {
		method = exch.getRequestMethod().toString();
		url = exch.getRequestURL();
		headers = new HashMap<>();
		for (HeaderValues h : exch.getRequestHeaders())
			headers.put(h.getHeaderName().toString(), h);
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

	public HttpRequest() {
		super();
	}

	public void request(HttpClient client, Consumer<HttpResponse> using) {
		ListenableFuture<com.ning.http.client.Response> f = client.requestBuilder(method, url).setBody(body).setHeaders(headers).setCookies(
				cookies.parallelStream().map(HttpClient::conv).collect(Collectors.toList())).execute();
		f.addListener(() -> {
			try {
				using.accept(new HttpResponse(f.get()));
			} catch (InterruptedException e) {
				return;
			} catch (ExecutionException e) {
				logger.error("HTTP request fail", e.getCause());
				return;
			}
		}, ForkJoinPool.commonPool());
	}

	@Override
	public HttpRequest save(OutputStream out) {
		try {
			IOs.writeBytes(out, method.getBytes());
			IOs.writeBytes(out, url.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return super.save(out);
	}

	@Override
	public HttpRequest load(InputStream in) {
		try {
			method = new String(IOs.readBytes(in));
			url = new String(IOs.readBytes(in));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return super.load(in);
	}

	public HttpRequest redirect(String host, int port) {
		try {
			URL orig = new URL(url);
			StringBuilder sb = new StringBuilder(orig.getProtocol()).append("://");
			if (orig.getUserInfo() != null) sb.append(orig.getUserInfo()).append("@");
			sb.append(host);
			if (port > 0) sb.append(":").append(port);
			sb.append(orig.getFile());
			if (orig.getRef() != null) sb.append("#").append(orig.getRef());
			url = new URL(sb.toString()).toString();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public String method() {
		return method;
	}
}
