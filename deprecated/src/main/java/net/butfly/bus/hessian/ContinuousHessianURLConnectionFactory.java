package net.butfly.bus.hessian;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;

public class ContinuousHessianURLConnectionFactory implements HessianConnectionFactory {
	private static final Logger logger = Logger.getLogger(ContinuousHessianURLConnectionFactory.class.getName());

	private HessianProxyFactory _proxyFactory;

	public void setHessianProxyFactory(HessianProxyFactory factory) {
		_proxyFactory = factory;
	}

	/**
	 * Opens a new or recycled connection to the HTTP server.
	 */
	public HessianConnection open(URL url) throws IOException {
		if (logger.isLoggable(Level.FINER)) logger.finer(this + " open(" + url + ")");

		URLConnection conn = url.openConnection();

		// HttpURLConnection httpConn = (HttpURLConnection) conn;
		// httpConn.setRequestMethod("POST");
		// conn.setDoInput(true);

		long connectTimeout = _proxyFactory.getConnectTimeout();
		if (connectTimeout >= 0) conn.setConnectTimeout((int) connectTimeout);
		long readTimeout = _proxyFactory.getReadTimeout();
		if (readTimeout > 0) {
			try {
				conn.setReadTimeout((int) readTimeout);
			} catch (Throwable e) {}
		}
		HttpURLConnection httpConn = (HttpURLConnection) conn;
		httpConn.setChunkedStreamingMode(8 * 1024);
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		return new ContinuousHessianURLConnection(url, conn);
	}
}
