package net.butfly.bus.invoker;

import java.lang.reflect.Method;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.albacore.utils.async.Task.Callable;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.TXUtils;
import net.butfly.bus.utils.TXUtils.TXImpl;

public abstract class AbstractLocalInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	public Method getMethod(String code, String version) {
		TXImpl key = this.scanTXInPools(TXUtils.TXImpl(code, version));
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");
		return METHOD_POOL.get(key);
	}

	@Override
	protected Callable<Response> task(Request request, Options[] options) {
		return new InvokeTask(request);
	}

	private class InvokeTask implements Task.Callable<Response> {
		private Request request;

		public InvokeTask(Request request) {
			this.request = request;
		}

		@Override
		public Response call() throws Exception {
			Response resp = new Response(request);
			if (auth != null) auth.login(AbstractLocalInvoker.this.token());
			TXImpl key = scanTXInPools(TXUtils.TXImpl(request.code(), request.version()));
			if (null == key)
				throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key
						+ "] not fould in registered txes: [" + METHOD_POOL.keySet().toString() + "].");

			Method method = METHOD_POOL.get(key);
			Object bean = INSTANCE_POOL.get(key);
			Object[] args = request.arguments();
			return resp.result(method.invoke(bean, args));
		}
	}

	private TXImpl scanTXInPools(TXImpl requestTX) {
		if (!TX_POOL.containsKey(requestTX.value())) return null;
		if (TX.ALL_VERSION.equals(requestTX)) return TX_POOL.get(requestTX.value()).first();
		return TX_POOL.get(requestTX.value()).ceiling(requestTX);
	}
}
