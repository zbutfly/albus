package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.invoker.WebServiceInvoker.HandlerResponse;
import net.butfly.bus.serialize.Serializers;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;

import com.google.common.net.HttpHeaders;

public class HttpUrlHandler extends HttpHandler {
	public HttpUrlHandler(int connTimeout, int readTimeout) {
		super(connTimeout, readTimeout);
	}

	@Override
	public HandlerResponse post(String url, Map<String, String> headers, byte[] data, String mimeType, Charset charset,
			boolean streaming) throws IOException {
		logRequest(url, headers, data, charset, streaming);
		URL u;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			throw new SystemException("", e);
		}
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setConnectTimeout(this.connTimeout);
		conn.setReadTimeout(this.readTimeout);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		if (streaming) conn.setChunkedStreamingMode(0);
		for (Entry<String, String> h : headers.entrySet())
			conn.setRequestProperty(h.getKey(), h.getValue());
		if (conn.getRequestProperty(HttpHeaders.ACCEPT_ENCODING) == null)
			conn.setRequestProperty(HttpHeaders.ACCEPT_ENCODING, "deflate");
		if (conn.getRequestProperty(HttpHeaders.CONTENT_TYPE) == null)
			conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, ContentType.create(mimeType, charset).toString());

		int statusCode = 500;
		IOUtils.write(data, conn.getOutputStream());
		conn.getOutputStream().flush();

		statusCode = conn.getResponseCode();
		if (statusCode != 200) throw new SystemException("", "Http resposne status code: " + statusCode);
		InputStream resp = conn.getInputStream();
		if ("deflate".equals(conn.getContentEncoding())) resp = new InflaterInputStream(resp, new Inflater(true));
		byte[] recv = IOUtils.toByteArray(resp);
		Map<String, List<String>> recvHeaders = conn.getHeaderFields();
		if (logger.isTraceEnabled()) {
			logger.trace("HTTP Response RECV <== HEADERS: " + recvHeaders);
			logger.trace("HTTP Response RECV <== CONTENT[" + recv.length + "]: " + new String(recv, contentType(conn)));
		}
		return new HandlerResponse(recvHeaders, recv);
	}

	private Charset contentType(HttpURLConnection conn) {
		String contentType = conn.getContentType();
		if (null == contentType) contentType = conn.getRequestProperty(HttpHeaders.CONTENT_TYPE);
		return null == contentType ? Serializers.DEFAULT_CHARSET : ContentType.parse(contentType).getCharset();
	}
}
