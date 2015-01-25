package net.butfly.bus.utils.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.butfly.bus.invoker.WebServiceInvoker.HandlerResponse;

import org.apache.http.entity.ContentType;

import com.google.common.net.HttpHeaders;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Response;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;

public class HttpNingHandler extends HttpHandler {
	AsyncHttpClient client;

	public HttpNingHandler() {
		this(0, 0);
	}

	public HttpNingHandler(int connTimeout, int readTimeout) {
		super(connTimeout, readTimeout);
		Builder b = new AsyncHttpClientConfig.Builder();
		if (connTimeout > 0) b.setConnectTimeout(connTimeout);
		if (readTimeout > 0) b.setReadTimeout(readTimeout);
		AsyncHttpClientConfig config = b.build();
		this.client = new AsyncHttpClient(new NettyAsyncHttpProvider(config));
	}

	@Override
	public HandlerResponse post(String url, Map<String, String> headers, byte[] data, String mimeType, Charset charset,
			boolean streaming) throws IOException {
		logRequest(url, headers, data, charset, streaming);

		BoundRequestBuilder req = this.client.preparePost(url);
		for (String name : headers.keySet())
			req.setHeader(name, headers.get(name));
		req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.create(mimeType, charset).toString());
		req.setHeader(HttpHeaders.ACCEPT_ENCODING, "deflate");
		req.setBody(data);
		Response resp;
		try {
			resp = req.execute().get();
		} catch (InterruptedException e) {
			throw new IOException("Async Http interrupted", e);
		} catch (ExecutionException e) {
			throw new IOException("Async Http failure", e.getCause());
		}

		int statusCode = resp.getStatusCode();
		if (statusCode != 200) throw new IOException("Async Http resposne status code: " + statusCode);

		Map<String, List<String>> recvHeaders = resp.getHeaders();
		byte[] recv = resp.getResponseBodyAsBytes();
		return new HandlerResponse(recvHeaders, recv);
	}
	//
	// private Charset contentType(HttpURLConnection conn) {
	// String contentType = conn.getContentType();
	// if (null == contentType) contentType = conn.getRequestProperty(HttpHeaders.CONTENT_TYPE);
	// return null == contentType ? Serializers.DEFAULT_CHARSET :
	// ContentType.parse(contentType).getCharset();
	// }
}
