package net.butfly.bus.impl;

import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.UtilsBase;
import net.butfly.bus.config.bean.invoker.InvokerBean;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.utils.Constants;

@SuppressWarnings("unchecked")
public class Invokers extends UtilsBase {
	private static Map<String, Invoker<?>> INVOKER_POOL = new HashMap<String, Invoker<?>>();

	static <C extends InvokerConfigBean> Invoker<C> getInvoker(InvokerBean bean) {
		return (Invoker<C>) INVOKER_POOL.get(key(bean));
	}

	public static <C extends InvokerConfigBean> void register(InvokerBean bean) {
		Class<? extends Invoker<C>> clazz = (Class<? extends Invoker<C>>) bean.type();
		String key = key(bean);
		if (INVOKER_POOL.containsKey(key)) return;
		try {
			Invoker<C> invoker = clazz.newInstance();
			invoker.initialize((C) bean.config(), bean.getToken());
			if (!bean.config().isLazy()) invoker.initialize();
			INVOKER_POOL.put(key, invoker);
		} catch (Throwable e) {
			throw new SystemException(Constants.UserError.NODE_CONFIG, "Invoker " + clazz.getName()
					+ " initialization failed for invalid Invoker instance.", e);
		}
	}

	private static <C extends InvokerConfigBean> String key(InvokerBean bean) {
		C config = (C) bean.config();
		String key = bean.id();
		if (null != config) key = key + ":" + config.toString();
		return key;
	}
}
