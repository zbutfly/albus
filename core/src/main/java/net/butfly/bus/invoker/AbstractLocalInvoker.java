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
import net.butfly.bus.TXes;
import net.butfly.bus.service.AwareService;
import net.butfly.bus.utils.Constants;

public abstract class AbstractLocalInvoker extends AbstractInvoker {
	protected Map<String, TreeSet<String>> TX_POOL = new HashMap<String, TreeSet<String>>();
	protected Map<String, Object> INSTANCE_POOL = new HashMap<String, Object>();
	protected Map<String, Method> METHOD_POOL = new HashMap<String, Method>();
	private Map<Class<? extends Service>, Service> AWARED_POOL = new HashMap<Class<? extends Service>, Service>();

	@SuppressWarnings("unchecked")
	@Override
	public void initialize() {
		if (!this.METHOD_POOL.isEmpty()) this.METHOD_POOL.clear();
		try {
			logger.trace("Invoker " + this.getClass().getName() + "[" + config + "] parsing...");
			Object[] beans = getBeanList();
			Class<?>[] awaredServiceClasses = Reflections.getAnnotatedTypes(AwareService.class);
			for (Object bean : beans) {
				Class<?> implClass = bean.getClass();
				/* DO not scan tx on implementation of facade.scanMethodsForTX(implClass, bean); */
				for (Class<?> clazz : implClass.getInterfaces())
					scanMethodsForTX(clazz, bean);
				for (Class<?> cl : awaredServiceClasses)
					if (cl.isInterface() && cl.isAssignableFrom(implClass))
						this.AWARED_POOL.put((Class<? extends Service>) cl, (Service) bean);
			}
			logger.trace("Invoker " + this.getClass().getName() + "[" + config + "] parsed.");
		} catch (Exception _ex) {
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, _ex);
		}
		super.initialize();
	}

	@Override
	public boolean lazy() {
		String l = System.getProperty("bus.invoker.spring.lazy");
		return Boolean.parseBoolean(l == null ? config.param("lazy") : l);
	}

	public Method getMethod(String code, String version) {
		String key = key(code, version);
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");
		return METHOD_POOL.get(key);
	}

	@Override
	public Response invoke(final Request request, final Options... remoteOptions) throws Exception {
		Response resp = new Response(request);
		String key = key(request.code(), request.version());
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
	public <S extends Service> S awared(Class<S> awaredServiceClass) {
		if (awaredServiceClass.isInterface()) return (S) this.AWARED_POOL.get(awaredServiceClass);
		throw new IllegalArgumentException("Can fetch awared bean by interface only.");
	}

	private String key(String code, String version) {
		if (TX_POOL.containsKey(code)) {
			if (TX.ALL_VERSION.equals(version)) return TXes.key(code, TX_POOL.get(code).first());
			return TXes.key(code, TX_POOL.get(code).ceiling(version));
		}
		if (config != null) {
			this.initialize();
			if (TX_POOL.containsKey(code)) {
				if (TX.ALL_VERSION.equals(version)) return TXes.key(code, TX_POOL.get(code).first());
				return TXes.key(code, TX_POOL.get(code).ceiling(version));
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
				if (null != tx) {
					String key = TXes.key(tx);
					logger.info("TX found: " + key + ".");
					if (METHOD_POOL.containsKey(key)) {
						logger.warn("TX duplicated: " + key + ", ignored...");
						continue;
					}
					if (!TX_POOL.containsKey(tx.value())) TX_POOL.put(tx.value(), new TreeSet<String>());
					if (TX_POOL.get(tx.value()).contains(tx.value())) {
						logger.warn("TX [" + tx.value() + "] version duplicated: " + tx.version() + ", ignored...");
						continue;
					}
					TX_POOL.get(tx.value()).add(tx.version());
					METHOD_POOL.put(key, m);
					if (!Modifier.isStatic(m.getModifiers())) INSTANCE_POOL.put(key, bean);
				}
			}
			clazz = clazz.getSuperclass();
		}
	}
}
