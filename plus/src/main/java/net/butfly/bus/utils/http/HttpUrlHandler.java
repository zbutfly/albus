package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.invoker.WebServiceInvoker.HandlerResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;

public class HttpUrlHandler extends HttpHandler {
	public HttpUrlHandler(int connTimeout, int readTimeout) {
		super(connTimeout, readTimeout);
	}

	@Override
	public HandlerResponse post(String url, Map<String, String> headers, byte[] data, ContentType contentType, boolean streaming)
			throws IOException {
		this.logRequest(url, headers, new String(data, contentType.getCharset()), streaming);
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
			conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, contentType.toString());

		int statusCode = 500;
		OutputStream req = conn.getOutputStream();

		IOUtils.write(data, req);
		req.flush();

		statusCode = conn.getResponseCode();
		if (statusCode != 200) throw new SystemException("", "Http resposne status code: " + statusCode);
		InputStream resp = conn.getInputStream();
		if ("deflate".equals(conn.getContentEncoding())) resp = new InflaterInputStream(resp, new Inflater(true));
		return new HandlerResponse(conn.getHeaderFields(), resp);
	}
}
