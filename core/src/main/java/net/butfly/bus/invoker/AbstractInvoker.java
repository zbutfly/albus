package net.butfly.bus.invoker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.argument.TX;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.facade.AuthFacade;
import net.butfly.bus.util.TXUtils;
import net.butfly.bus.util.TXUtils.TXImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInvoker<C extends InvokerConfigBean> implements Invoker<C> {
	protected static Logger logger = LoggerFactory.getLogger(Invoker.class);

	protected Map<String, TreeSet<TXImpl>> TX_POOL = new HashMap<String, TreeSet<TXImpl>>();
	protected Map<TXImpl, Object> INSTANCE_POOL = new HashMap<TXImpl, Object>();
	protected Map<TXImpl, Method> METHOD_POOL = new HashMap<TXImpl, Method>();
	protected AuthFacade auth;
	protected Token token;

	@Override
	public String[] getTXCodes() {
		return TX_POOL.keySet().toArray(new String[TX_POOL.keySet().size()]);
	}

	@Override
	public void initialize(C config, Token token) {
		this.token = token;
		if (this.METHOD_POOL.isEmpty()) try {
			logger.trace("Invoker parsing...");
			for (Object bean : getBeanList()) {
				Class<?> implClass = bean.getClass();
				// DO not scan tx on implementation of facade.
				// scanMethodsForTX(implClass, bean);
			for (Class<?> clazz : implClass.getInterfaces())
				scanMethodsForTX(clazz, bean);
			if (AuthFacade.class.isAssignableFrom(implClass)) this.auth = (AuthFacade) bean;
		}
		logger.trace("Invoker parsed.");
	} catch (Exception _ex) {
		throw new SystemException(Constants.BusinessError.CONFIG_ERROR, _ex);
	}
	}

	private void scanMethodsForTX(Class<?> clazz, Object bean) throws SecurityException, NoSuchMethodException {
		while (clazz != null && !clazz.equals(Object.class)) {
			for (Method m : clazz.getDeclaredMethods()) {
				TX tx = m.getAnnotation(TX.class);
				if (tx != null) {
					TXImpl key = TXUtils.TXImpl(tx);
					logger.trace("TX found: " + key + ".");
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
}
