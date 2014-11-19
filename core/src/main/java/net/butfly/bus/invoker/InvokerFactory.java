package net.butfly.bus.invoker;

import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.GenericUtils;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.config.bean.invoker.InvokerBean;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public class InvokerFactory {
	@SuppressWarnings("unchecked")
	public static InvokerConfigBean getConfig(Class<? extends Invoker<?>> invokerClass) {
		Class<? extends InvokerConfigBean> configClass = (Class<? extends InvokerConfigBean>) GenericUtils
				.getGenericParamClass(invokerClass, Invoker.class, "C");
		try {
			return configClass.newInstance();
		} catch (Throwable e) {
			return null;
		}
	}

	private static Map<String, Invoker<?>> INVOKER_POOL = new HashMap<String, Invoker<?>>();

	@SuppressWarnings("unchecked")
	public static <C extends InvokerConfigBean> Invoker<C> getInvoker(InvokerBean bean) {
		Class<? extends Invoker<C>> clazz = (Class<? extends Invoker<C>>) bean.type();
		C config = (C) bean.config();
		String key = clazz.getName();
		if (null != config) key = key + ":" + config.toString();
		if (INVOKER_POOL.containsKey(key)) return (Invoker<C>) INVOKER_POOL.get(key);
		try {
			Invoker<C> invoker = clazz.newInstance();
			invoker.initialize(config, bean.getToken());
			INVOKER_POOL.put(key, invoker);
			return invoker;
		} catch (Throwable e) {
			throw new SystemException(Constants.UserError.NODE_CONFIG, "Invoker " + clazz.getName()
					+ " initialization failed for invalid Invoker instance.", e);
		}
	}
}
