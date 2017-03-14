package net.butfly.bus.utils.http;

import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProvider;

public final class HttpClient extends com.ning.http.client.AsyncHttpClient {
	public HttpClient() {
		super();
	}

	HttpClient(AsyncHttpClientConfig config) {
		super(config);
	}

	HttpClient(AsyncHttpProvider httpProvider, AsyncHttpClientConfig config) {
		super(httpProvider, config);
	}

	HttpClient(AsyncHttpProvider provider) {
		super(provider);
	}

	HttpClient(String providerClass, AsyncHttpClientConfig config) {
		super(providerClass, config);
	}

	@Override
	public BoundRequestBuilder requestBuilder(String method, String url) {
		return super.requestBuilder(method, url);
	}

	public static javax.servlet.http.Cookie unconv(com.ning.http.client.cookie.Cookie c) {
		javax.servlet.http.Cookie cc = new javax.servlet.http.Cookie(c.getName(), c.getValue());
		cc.setDomain(c.getDomain());
		cc.setPath(c.getPath());
		cc.setMaxAge((int) c.getMaxAge());
		cc.setHttpOnly(c.isHttpOnly());
		cc.setSecure(c.isSecure());
		return cc;
	}

	public static com.ning.http.client.cookie.Cookie conv(javax.servlet.http.Cookie c) {
		return com.ning.http.client.cookie.Cookie.newValidCookie(c.getName(), c.getValue(), false, c.getDomain(), c.getPath(), c
				.getMaxAge(), c.getSecure(), c.isHttpOnly());
	}
}