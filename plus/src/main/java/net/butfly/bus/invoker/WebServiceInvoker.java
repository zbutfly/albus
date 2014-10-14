package net.butfly.bus.invoker;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.http.HttpClientFactory;
import net.butfly.bus.Constants;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.invoker.WebServiceInvokerConfig;
import net.butfly.bus.deploy.entry.EntryPoint;
import net.butfly.bus.ext.AsyncRequest;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.AbstractSerializerFactory;

public class WebServiceInvoker extends AbstractRemoteInvoker<WebServiceInvokerConfig> implements Invoker<WebServiceInvokerConfig> {
	private static Logger logger = LoggerFactory.getLogger(WebServiceInvoker.class);
	private static long DEFAULT_TIMEOUT = 5000;
	private String path;
	private long timeout;

	private HttpClient client = HttpClientFactory.createSafeHttpClient();

	@Override
	public void initialize(WebServiceInvokerConfig config, Token token) {
		this.path = config.getPath();
		this.timeout = config.getTimeout() > 0 ? config.getTimeout() : DEFAULT_TIMEOUT;
		super.initialize(config, token);

		this.factory = new HessianProxyFactory();
		this.factory.setConnectTimeout(timeout);
		for (AbstractSerializerFactory s : this.createSerializers())
			this.factory.getSerializerFactory().addFactory(s);
		try {
			this.proxy = (EntryPoint) this.factory.create(EntryPoint.class, path);
		} catch (MalformedURLException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "HessianSerializer url [" + path + "] invalid.", ex);
		}

		if (this.continuousSupported()) {
			this.asyncFactory = new ContinuousHessianProxyFactory();
			// this.asyncFactory.setConnectTimeout(timeout);
			for (AbstractSerializerFactory s : this.createSerializers())
				this.asyncFactory.getSerializerFactory().addFactory(s);
		}

	}

	private List<AbstractSerializerFactory> createSerializers() {
		List<AbstractSerializerFactory> list = new ArrayList<AbstractSerializerFactory>(this.translators.size());
		for (Class<? extends AbstractSerializerFactory> clazz : this.translators) {
			try {
				list.add(clazz.newInstance());
			} catch (Exception ex) {
				logger.error("Type translator for hessian [" + clazz.getName() + "] invalid, ignored.");
			}
		}
		return list;
	}

	@Override
	public Response invoke(Request request) {
		logger.trace("Attemp hessian connection: " + path + ".");
		try {
			return super.invoke(request);
		} catch (HessianConnectionException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "HessianSerializer connection [" + path + "] invalid.",
					ex);
		} catch (HessianRuntimeException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "HessianSerializer service [" + path + "] invalid.",
					ex.getCause());
		}

	}

	protected void continuousInvoke(AsyncRequest request) {
		if (!this.continuousSupported())
			throw new UnsupportedOperationException("Invoker not configurated as continuous supported.");
		EntryPoint proxy;
		try {
			proxy = (EntryPoint) this.asyncFactory.create(EntryPoint.class, path, request);
		} catch (MalformedURLException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "HessianSerializer url [" + path + "] invalid.", ex);
		}
		proxy.invoke(request.request(this.token));
	}

	protected Response singleInvoke(Request request) {
		return proxy.invoke(request);
	}

	private void initHttpClient() {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
                socketFactoryRegistry, connFactory, dnsResolver);

	}
}
