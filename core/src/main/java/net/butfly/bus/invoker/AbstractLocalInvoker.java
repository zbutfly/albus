package net.butfly.bus.invoker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.service.Service;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.service.AwareService;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.TXUtils;
import net.butfly.bus.utils.TXUtils.TXImpl;

public abstract class AbstractLocalInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	protected Map<String, TreeSet<TXImpl>> TX_POOL = new HashMap<String, TreeSet<TXImpl>>();
	protected Map<TXImpl, Object> INSTANCE_POOL = new HashMap<TXImpl, Object>();
	protected Map<TXImpl, Method> METHOD_POOL = new HashMap<TXImpl, Method>();
	private Map<Class<? extends Service>, Service> AWARED_POOL = new HashMap<Class<? extends Service>, Service>();

	@SuppressWarnings("unchecked")
	@Override
	public void initialize() {
		if (this.METHOD_POOL.isEmpty())
			try {
				logger.trace("Invoker " + this.getClass().getName() + "[" + config + "] parsing...");
				Object[] beans = getBeanList();
				Class<?>[] serviceClasses = Reflections.getAnnotatedTypes(AwareService.class);
				for (Object bean : beans) {
					Class<?> implClass = bean.getClass();
					/* DO not scan tx on implementation of facade.scanMethodsForTX(implClass, bean); */
					for (Class<?> clazz : implClass.getInterfaces())
						scanMethodsForTX(clazz, bean);
					for (Class<?> cl : serviceClasses)
						if (cl.isInterface() && cl.isAssignableFrom(implClass))
							this.AWARED_POOL.put((Class<? extends Service>) cl, (Service) bean);
				}
				logger.trace("Invoker " + this.getClass().getName() + "[" + config + "] parsed.");
			} catch (Exception _ex) {
				throw new SystemException(Constants.BusinessError.CONFIG_ERROR, _ex);
			}
		super.initialize();
	}

	public Method getMethod(String code, String version) {
		TXImpl key = this.scanTXLazily(TXUtils.TXImpl(code, version));
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");
		return METHOD_POOL.get(key);
	}

	@Override
	public Response invoke(final Request request, final Options... remoteOptions) throws Exception {
		Response resp = new Response(request);
		// if (auth != null) auth.login(AbstractLocalInvoker.this.token());
		TXImpl key = scanTXLazily(TXUtils.TXImpl(request.code(), request.version()));
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");

		Method method = METHOD_POOL.get(key);
		Object bean = INSTANCE_POOL.get(key);
		Object[] args = request.arguments();
		return resp.result(method.invoke(bean, args));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends Service> S awared(Class<S> serviceClass) {
		if (serviceClass.isInterface()) return (S) this.AWARED_POOL.get(serviceClass);
		throw new IllegalArgumentException("Can fetch awared bean by interface only.");
	}

	private TXImpl scanTXLazily(TXImpl requestTX) {
		if (TX_POOL.containsKey(requestTX.value())) {
			if (TX.ALL_VERSION.equals(requestTX)) return TX_POOL.get(requestTX.value()).first();
			return TX_POOL.get(requestTX.value()).ceiling(requestTX);
		}
		if (config != null) {
			this.initialize();
			if (TX_POOL.containsKey(requestTX.value())) {
				if (TX.ALL_VERSION.equals(requestTX)) return TX_POOL.get(requestTX.value()).first();
				return TX_POOL.get(requestTX.value()).ceiling(requestTX);
			}
		}
		return null;
	}

	@Override
	public final Options[] remoteOptions(Options... options) {
		return new Options[0];
	}

	@Override
	public Options localOptions(Options... options) {
		return options == null || options.length == 0 ? new Options() : options[0];
	}

	@Override
	public boolean isSupported(String tx) {
		return TX_POOL.keySet().contains(tx);
	}

	private void scanMethodsForTX(Class<?> clazz, Object bean) throws SecurityException, NoSuchMethodException {
		while (clazz != null && !clazz.equals(Object.class)) {
			for (Method m : clazz.getDeclaredMethods()) {
				TX tx = m.getAnnotation(TX.class);
				if (tx != null) {
					TXImpl key = TXUtils.TXImpl(tx);
					logger.info("TX found: " + key + ".");
					if (METHOD_POOL.containsKey(key)) {
						logger.warn("TX duplicated: " + key + ", ignored...");
						continue;
					}
					if (!TX_POOL.containsKey(key.value())) TX_POOL.put(key.value(), new TreeSet<TXImpl>());
					if (TX_POOL.get(key.value()).contains(key)) {
						logger.warn("TX [" + key.value() + "] version duplicated: " + key.version() + ", ignored...");
						continue;
					}
					TX_POOL.get(key.value()).add(key);
					METHOD_POOL.put(key, m);
					if (!Modifier.isStatic(m.getModifiers())) INSTANCE_POOL.put(key, bean);
				}
			}
			clazz = clazz.getSuperclass();
		}
	}
}
