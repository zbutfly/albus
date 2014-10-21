package net.butfly.bus.hessian;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.argument.AsyncRequest;

import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.io.HessianRemoteObject;

public class ContinuousHessianProxyFactory extends HessianProxyFactory {
	private ClassLoader _loader;

	public ContinuousHessianProxyFactory() {
		super();
		this._loader = Thread.currentThread().getContextClassLoader();
	}

	@SuppressWarnings("unchecked")
	public <T, R> T create(Class<T> api, String path, AsyncRequest request)
			throws MalformedURLException {
		URL url = new URL(path);
		if (api == null) throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
		InvocationHandler handler = null;
		handler = new ContinuousHessianProxy<R>(url, this, api, (AsyncCallback<R>) request.callback(), request);
		return (T) Proxy.newProxyInstance(this._loader, new Class[] { api, HessianRemoteObject.class }, handler);
	}

	@Override
	protected HessianConnectionFactory createHessianConnectionFactory() {
		String className = System.getProperty(HessianConnectionFactory.class.getName());
		if (className == null) return new ContinuousHessianURLConnectionFactory();
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> cl = Class.forName(className, false, loader);
			return (HessianConnectionFactory) cl.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
