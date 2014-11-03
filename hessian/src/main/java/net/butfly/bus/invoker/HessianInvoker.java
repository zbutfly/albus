package net.butfly.bus.invoker;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.argument.AsyncRequest;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.invoker.HessianInvokerConfig;
import net.butfly.bus.deploy.entry.EntryPoint;
import net.butfly.bus.hessian.ContinuousHessianProxyFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.client.HessianConnectionException;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.AbstractSerializerFactory;

public class HessianInvoker extends AbstractRemoteInvoker<HessianInvokerConfig> implements Invoker<HessianInvokerConfig> {
	private static Logger logger = LoggerFactory.getLogger(HessianInvoker.class);
	private static long DEFAULT_TIMEOUT = 5000;
	private String path;
	private long timeout;
	private List<Class<? extends AbstractSerializerFactory>> translators;

	private HessianProxyFactory factory;
	private EntryPoint proxy;
	private ContinuousHessianProxyFactory asyncFactory;

	@Override
	public void initialize(HessianInvokerConfig config, Token token) {
		this.path = config.getPath();
		this.timeout = config.getTimeout() > 0 ? config.getTimeout() : DEFAULT_TIMEOUT;
		this.translators = config.getTypeTranslators();
		super.initialize(config, token);

		this.factory = new HessianProxyFactory();
		this.factory.setConnectTimeout(timeout);
		for (AbstractSerializerFactory s : this.createSerializers())
			this.factory.getSerializerFactory().addFactory(s);
		try {
			this.proxy = (EntryPoint) this.factory.create(EntryPoint.class, path);
		} catch (MalformedURLException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION,
					"HessianSerializer url [" + path + "] invalid.", ex);
		}
		this.asyncFactory = new ContinuousHessianProxyFactory();
		// this.asyncFactory.setConnectTimeout(timeout);
		for (AbstractSerializerFactory s : this.createSerializers())
			this.asyncFactory.getSerializerFactory().addFactory(s);
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
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "HessianSerializer connection [" + path
					+ "] invalid.", ex);
		} catch (HessianRuntimeException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "HessianSerializer service [" + path
					+ "] invalid.", ex.getCause());
		}

	}

	protected void asyncInvoke(AsyncRequest request) {
		EntryPoint proxy;
		try {
			proxy = (EntryPoint) this.asyncFactory.create(EntryPoint.class, path, request);
		} catch (MalformedURLException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION,
					"HessianSerializer url [" + path + "] invalid.", ex);
		}
		request.token(this.token());
		proxy.invoke(request);
	}

	protected Response syncInvoke(Request request) {
		return proxy.invoke(request);
	}
}
