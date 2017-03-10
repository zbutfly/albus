package net.butfly.bus.utils.gap;

import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProvider;

final class HttpClient extends com.ning.http.client.AsyncHttpClient {
	HttpClient() {
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
}