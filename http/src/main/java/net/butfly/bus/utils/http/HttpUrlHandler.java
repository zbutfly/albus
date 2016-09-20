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

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;

import com.google.common.net.HttpHeaders;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.serder.Serders;
import net.butfly.albacore.serder.ArrableTextSerder;

public class HttpUrlHandler extends HttpHandler {
	public HttpUrlHandler(ArrableTextSerder<Object> serializer, int connTimeout, int readTimeout) {
		super(serializer);
	}

	@Override
	public ResponseHandler post(BusHttpRequest httpRequest) throws IOException {
		httpRequest.logRequest(logger);
		URL u;
		try {
			u = new URL(httpRequest.url);
		} catch (MalformedURLException e) {
			throw new SystemException("", e);
		}
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		if (httpRequest.timeout > 0) {
			// conn.setConnectTimeout(httpRequest.timeout);
			conn.setReadTimeout(httpRequest.timeout);
		}
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setChunkedStreamingMode(0);
		for (Entry<String, String> h : httpRequest.headers.entrySet())
			conn.setRequestProperty(h.getKey(), h.getValue());
		if (conn.getRequestProperty(HttpHeaders.ACCEPT_ENCODING) == null) conn.setRequestProperty(HttpHeaders.ACCEPT_ENCODING, "deflate");
		if (conn.getRequestProperty(HttpHeaders.CONTENT_TYPE) == null) conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, ContentType.create(
				httpRequest.mimeType, httpRequest.charset).toString());

		int statusCode = 500;
		IOUtils.write(httpRequest.data, conn.getOutputStream());
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
		return new ResponseHandler(serializer, recvHeaders, recv);
	}

	private Charset contentType(HttpURLConnection conn) {
		String contentType = conn.getContentType();
		if (null == contentType) contentType = conn.getRequestProperty(HttpHeaders.CONTENT_TYPE);
		return null == contentType ? Serders.DEFAULT_CONTENT_TYPE.getCharset() : ContentType.parse(contentType).getCharset();
	}
}
