package net.butfly.bus.invoker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.service.Service;
import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.TX;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.context.Context;
import net.butfly.bus.service.AwareService;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.TXUtils;
import net.butfly.bus.utils.TXUtils.TXImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInvoker<C extends InvokerConfigBean> implements Invoker<C> {
	protected static Logger logger = LoggerFactory.getLogger(Invoker.class);
	protected C config;
	private Token token;

	protected Map<String, TreeSet<TXImpl>> TX_POOL = new HashMap<String, TreeSet<TXImpl>>();
	protected Map<TXImpl, Object> INSTANCE_POOL = new HashMap<TXImpl, Object>();
	protected Map<TXImpl, Method> METHOD_POOL = new HashMap<TXImpl, Method>();
	private Map<Class<? extends Service>, Service> AWARED_POOL = new HashMap<Class<? extends Service>, Service>();

	@Override
	public String[] getTXCodes() {
		return TX_POOL.keySet().toArray(new String[TX_POOL.keySet().size()]);
	}

	@Override
	public void initialize(C config, Token token) {
		this.config = config;
		this.token = token;
	}

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
					/* DO not scan tx on implementation of facade. scanMethodsForTX(implClass, bean); */
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
		this.config = null;
	}

	public boolean initialized() {
		return this.config == null;
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

	public void setToken(Token token) {
		this.token = token;
	}

	@Override
	public final Token token() {
		Token t = Context.token();
		return null == t ? this.token : t;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends Service> S awared(Class<S> serviceClass) {
		if (serviceClass.isInterface()) return (S) this.AWARED_POOL.get(serviceClass);
		throw new IllegalArgumentException("Can fetch awared bean by interface only.");
	}
}
