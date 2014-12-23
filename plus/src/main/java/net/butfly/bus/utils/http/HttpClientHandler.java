package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import net.butfly.albacore.exception.SystemException;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

public class HttpClientHandler extends HttpHandler {
	private CloseableHttpClient client;

	public HttpClientHandler(int connTimeout, int readTimeout) {
		super(connTimeout, readTimeout);
		client = HTTPUtils.createFull(this.connTimeout, this.readTimeout, -1);
	}

	@Override
	public InputStream post(String url, Map<String, String> headers, byte[] data, ContentType contentType, boolean streaming)
			throws IOException {
		HttpPost postReq = new HttpPost(url);

		for (Entry<String, String> h : headers.entrySet())
			postReq.setHeader(h.getKey(), h.getValue());
		if (postReq.getHeaders(HttpHeaders.ACCEPT_ENCODING) == null) postReq.setHeader(HttpHeaders.ACCEPT_ENCODING, "deflate");
		if (postReq.getHeaders(HttpHeaders.CONTENT_TYPE) == null)
			postReq.setHeader(HttpHeaders.CONTENT_TYPE, contentType.toString());

		ByteArrayEntity e = new ByteArrayEntity(data, contentType);
		// if (streaming) e.setChunked(true);
		postReq.setEntity(e);
		CloseableHttpResponse postResp = this.client.execute(postReq);
		try {
			if (postResp.getStatusLine().getStatusCode() != 200)
				throw new SystemException("", "HTTP failure: " + postResp.getStatusLine().getReasonPhrase());
			return postResp.getEntity().getContent();
		} finally {
			postResp.close();
		}
	}
}
