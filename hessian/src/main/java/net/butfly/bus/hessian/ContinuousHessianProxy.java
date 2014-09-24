package net.butfly.bus.hessian;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.WeakHashMap;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.ext.AsyncRequest;
import net.butfly.bus.util.async.AsyncInvokeUtils;
import net.butfly.bus.util.async.HandledBySignal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.HessianRemote;
import com.caucho.services.server.AbstractSkeleton;

public class ContinuousHessianProxy<R> implements InvocationHandler, Serializable {
	private static final long serialVersionUID = -8019595632297646789L;
	private static final Logger log = LoggerFactory.getLogger(ContinuousHessianProxy.class);
	protected ContinuousHessianProxyFactory _factory;
	private WeakHashMap<Method, String> _mangleMap = new WeakHashMap<Method, String>();
	private Class<?> _type;
	private URL _url;
	private final AsyncCallback<R> callback;
	private final AsyncRequest request;

	protected ContinuousHessianProxy(URL url, ContinuousHessianProxyFactory factory, AsyncCallback<R> callback,
			AsyncRequest request) {
		this(url, factory, null, callback, request);
	}

	protected ContinuousHessianProxy(URL url, ContinuousHessianProxyFactory factory, Class<?> type, AsyncCallback<R> callback,
			AsyncRequest request) {
		_factory = factory;
		_url = url;
		_type = type;
		this.request = request;
		this.callback = callback;
	}

	/**
	 * Returns the proxy's URL.
	 */
	public URL getURL() {
		return _url;
	}

	/**
	 * Handles the object invocation.
	 *
	 * @param proxy
	 *            the proxy object to invoke
	 * @param method
	 *            the method to call
	 * @param args
	 *            the arguments to the proxy object
	 */
	@SuppressWarnings({ "unchecked" })
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		String mangleName;
		synchronized (_mangleMap) {
			mangleName = _mangleMap.get(method);
		}
		if (mangleName == null) {
			String methodName = method.getName();
			Class<?>[] params = method.getParameterTypes();
			// equals and hashCode are special cased
			if (methodName.equals("equals") && params.length == 1 && params[0].equals(Object.class)) {
				Object value = args[0];
				if (value == null || !Proxy.isProxyClass(value.getClass())) return Boolean.FALSE;
				Object proxyHandler = Proxy.getInvocationHandler(value);
				if (!(proxyHandler instanceof ContinuousHessianProxy)) return Boolean.FALSE;
				ContinuousHessianProxy<R> handler = (ContinuousHessianProxy<R>) proxyHandler;
				return new Boolean(_url.equals(handler.getURL()));
			} else if (methodName.equals("hashCode") && params.length == 0) return new Integer(_url.hashCode());
			else if (methodName.equals("getHessianType")) return proxy.getClass().getInterfaces()[0].getName();
			else if (methodName.equals("getHessianURL")) return _url.toString();
			else if (methodName.equals("toString") && params.length == 0) return "ContinuousHessianProxy[" + _url + "]";
			if (!_factory.isOverloadEnabled()) mangleName = method.getName();
			else mangleName = mangleName(method);
			synchronized (_mangleMap) {
				_mangleMap.put(method, mangleName);
			}
		}
		InputStream iss = null;
		HessianConnection conn = null;
		try {
			conn = sendRequest(mangleName, args);
			final InputStream is = getInputStream(conn);
			iss = is;
			final Hessian2Input in = (Hessian2Input) _factory.getHessian2Input(is);

			AsyncInvokeUtils.handleBySignal(new HandledBySignal(this.request) {
				@Override
				public void handle() throws Throwable {
					int code = is.read();
					int major = is.read();
					int minor = is.read();
					if (code != 'H' || major != 2 || minor != 0)
						throw new HessianProtocolException("we do not support this version of hessian protocal.");
					R response = (R) in.readReply(method.getReturnType());
					ContinuousHessianProxy.this.callback.callback(response);
				}
			});
			return null;

		} catch (HessianProtocolException e) {
			throw new HessianRuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			// finally should not be touched on continuous invoking,
			// unless signal or exception is thrown.
			if (iss != null) try {
				iss.close();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			if (conn != null) try {
				conn.destroy();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	protected InputStream getInputStream(HessianConnection conn) throws IOException {
		InputStream is = conn.getInputStream();
		if ("deflate".equals(conn.getContentEncoding())) {
			is = new InflaterInputStream(is, new Inflater(true));
		}
		return is;
	}

	protected String mangleName(Method method) {
		Class<?>[] param = method.getParameterTypes();
		if (param == null || param.length == 0) return method.getName();
		else return AbstractSkeleton.mangleName(method, false);
	}

	/**
	 * Sends the HTTP request to the Hessian connection.
	 */
	protected HessianConnection sendRequest(String methodName, Object[] args) throws IOException {
		HessianConnection conn = null;
		conn = _factory.getConnectionFactory().open(_url);
		boolean isValid = false;
		try {
			addRequestHeaders(conn);
			OutputStream os = null;
			try {
				os = conn.getOutputStream();
			} catch (Exception e) {
				throw new HessianRuntimeException(e);
			}
			AbstractHessianOutput out = _factory.getHessianOutput(os);
			out.call(methodName, args);
			out.flush();
			conn.sendRequest();
			isValid = true;
			return conn;
		} finally {
			if (!isValid && conn != null) conn.destroy();
		}
	}

	/**
	 * Method that allows subclasses to add request headers such as cookies.
	 * Default implementation is empty.
	 */
	protected void addRequestHeaders(HessianConnection conn) {
		conn.addHeader("Content-Type", "x-application/hessian");
		conn.addHeader("Accept-Encoding", "deflate");
		String basicAuth = _factory.getBasicAuth();
		if (basicAuth != null) conn.addHeader("Authorization", basicAuth);
	}

	/**
	 * Method that allows subclasses to parse response headers such as cookies.
	 * Default implementation is empty.
	 * 
	 * @param conn
	 */
	protected void parseResponseHeaders(URLConnection conn) {}

	public Object writeReplace() {
		return new HessianRemote(_type.getName(), _url.toString());
	}
}
