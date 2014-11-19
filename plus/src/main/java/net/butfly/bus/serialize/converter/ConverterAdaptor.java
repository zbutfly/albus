package net.butfly.bus.serialize.converter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.butfly.albacore.exception.SystemException;

public abstract class ConverterAdaptor<CC> {
	private static Map<Class<? extends Converter<?, ?>>, Converter<?, ?>> POOL = new ConcurrentHashMap<Class<? extends Converter<?, ?>>, Converter<?, ?>>();

	@SuppressWarnings("unchecked")
	protected <C extends Converter<?, ?>> C getConverter(Class<C> clazz) {
		C c = (C) POOL.get(clazz);
		if (null == c) {
			try {
				c = clazz.newInstance();
			} catch (Exception e) {
				throw new SystemException("", e);
			}
			POOL.put(clazz, c);
		}
		return c;
	}

	public abstract <SRC, DST> CC create(Class<? extends Converter<SRC, DST>> converterClass);
}
