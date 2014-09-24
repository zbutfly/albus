package net.butfly.bus.invoker;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Constants;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.invoker.HessianInvokerConfig;
import net.butfly.bus.deploy.entry.EntryPoint;
import net.butfly.bus.ext.AsyncRequest;
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
	private ContinuousHessianProxyFactory asyncFactory;

	@Override
	public void initialize(HessianInvokerConfig config) {
		this.path = config.getPath();
		this.timeout = config.getTimeout() > 0 ? config.getTimeout() : DEFAULT_TIMEOUT;
		this.translators = config.getTypeTranslators();
		super.initialize(config);

		this.factory = new HessianProxyFactory();
		this.factory.setConnectTimeout(timeout);
		for (AbstractSerializerFactory s : this.createSerializers())
			this.factory.getSerializerFactory().addFactory(s);

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
			if (!(request instanceof AsyncRequest)) return singleInvoke(request);
			AsyncRequest areq = (AsyncRequest) request;
			if (!areq.continuous()) return singleInvoke(areq.request());
			this.continuousInvoke(areq);
		} catch (MalformedURLException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "Hessian url [" + path + "] invalid.", ex);
		} catch (HessianConnectionException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "Hessian connection [" + path + "] invalid.",
					ex);
		} catch (HessianRuntimeException ex) {
			throw new SystemException(Constants.SystemError.HESSIAN_CONNECTION, "Hessian service [" + path + "] invalid.",
					ex.getCause());
		}
		throw new IllegalAccessError("A continuous invoking should not end without exception.");
	}

	private void continuousInvoke(AsyncRequest request) throws MalformedURLException {
		if (!this.continuousSupported())
			throw new UnsupportedOperationException("Invoker not configurated as continuous supported.");
		EntryPoint proxy = (EntryPoint) this.asyncFactory.create(EntryPoint.class, path, request.callback(), request);
		proxy.invoke(request.request());
	}

	private Response singleInvoke(Request request) throws MalformedURLException {
		EntryPoint proxy = (EntryPoint) this.factory.create(EntryPoint.class, path);
		return proxy.invoke(request);
	}
}
