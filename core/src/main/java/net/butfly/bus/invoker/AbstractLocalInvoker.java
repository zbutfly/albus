package net.butfly.bus.invoker;

import java.lang.reflect.Method;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.TXUtils;
import net.butfly.bus.utils.TXUtils.TXImpl;

public abstract class AbstractLocalInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
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
		if (auth != null) auth.login(AbstractLocalInvoker.this.token());
		TXImpl key = scanTXLazily(TXUtils.TXImpl(request.code(), request.version()));
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");

		Method method = METHOD_POOL.get(key);
		Object bean = INSTANCE_POOL.get(key);
		Object[] args = request.arguments();
		return resp.result(method.invoke(bean, args));
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
}
