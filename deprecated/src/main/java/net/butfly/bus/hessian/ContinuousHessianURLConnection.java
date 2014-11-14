package net.butfly.bus.hessian;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import com.caucho.hessian.client.AbstractHessianConnection;
import com.caucho.hessian.client.HessianConnectionException;

public class ContinuousHessianURLConnection extends AbstractHessianConnection {
	private URL _url;
	private URLConnection _conn;
	private int _statusCode;
	private String _statusMessage;

	ContinuousHessianURLConnection(URL url, URLConnection conn) {
		_url = url;
		_conn = conn;
	}

	/**
	 * Adds a HTTP header.
	 */
	@Override
	public void addHeader(String key, String value) {
		_conn.setRequestProperty(key, value);
	}

	/**
	 * Returns the output stream for the request.
	 */
	public OutputStream getOutputStream() throws IOException {
		return _conn.getOutputStream();
	}

	/**
	 * Sends the request
	 */
	public void sendRequest() throws IOException {
		if (_conn instanceof HttpURLConnection) {
			HttpURLConnection httpConn = (HttpURLConnection) _conn;
			_statusCode = 500;
			try {
				_statusCode = httpConn.getResponseCode();
			} catch (Exception e) {}
			parseResponseHeaders(httpConn);
			InputStream is = null;
			if (_statusCode != 200) {
				StringBuffer sb = new StringBuffer();
				int ch;
				try {
					is = httpConn.getInputStream();
					if (is != null) {
						while ((ch = is.read()) >= 0)
							sb.append((char) ch);
						is.close();
					}
					is = httpConn.getErrorStream();
					if (is != null) {
						while ((ch = is.read()) >= 0)
							sb.append((char) ch);
					}
					_statusMessage = sb.toString();
				} catch (FileNotFoundException e) {
					throw new HessianConnectionException("HessianProxy cannot connect to '" + _url, e);
				} catch (IOException e) {
					if (is == null) throw new HessianConnectionException(_statusCode + ": " + e, e);
					else throw new HessianConnectionException(_statusCode + ": " + sb, e);
				}
				if (is != null) is.close();
				throw new HessianConnectionException(_statusCode + ": " + sb.toString());
			}
		}
	}

	protected void parseResponseHeaders(HttpURLConnection conn) throws IOException {}

	/**
	 * Returns the status code.
	 */
	public int getStatusCode() {
		return _statusCode;
	}

	/**
	 * Returns the status string.
	 */
	public String getStatusMessage() {
		return _statusMessage;
	}

	/**
	 * Returns the InputStream to the result
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return _conn.getInputStream();
	}

	@Override
	public String getContentEncoding() {
		return _conn.getContentEncoding();
	}

	/**
	 * Close/free the connection
	 */
	@Override
	public void close() {}

	/**
	 * Disconnect the connection
	 */
	@Override
	public void destroy() {
		close();
		URLConnection conn = _conn;
		_conn = null;
		if (conn instanceof HttpURLConnection) ((HttpURLConnection) conn).disconnect();
	}
}
