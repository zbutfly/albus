package net.butfly.bus.invoker;

import java.lang.reflect.Method;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.ExceptionUtils;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Error;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.context.Context;
import net.butfly.bus.utils.BusTask;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.TXUtils;
import net.butfly.bus.utils.TXUtils.TXImpl;

public abstract class AbstractLocalInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	@Override
	public Mode mode() {
		return Mode.SERVER;
	}

	public Method getMethod(String code, String version) {
		TXImpl key = this.scanTXInPools(TXUtils.TXImpl(code, version));
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");
		return METHOD_POOL.get(key);
	}

	@Override
	public Response invoke(final Request request, final Options... options) throws Exception {
		return new BusTask<Response>(new InvokeTask(request), this.localOptions(options)).execute();
	}

	@Override
	public void invoke(final Request request, final Task.Callback<Response> callback, final Options... options)
			throws Exception {
		new BusTask<Response>(new InvokeTask(request), callback, this.localOptions(options)).execute();
	}

	private class InvokeTask implements Task.Callable<Response> {
		private Request request;

		public InvokeTask(Request request) {
			this.request = request;
		}

		@Override
		public Response call() {
			Response resp = new Response(request);
			try {
				if (auth != null) auth.login(AbstractLocalInvoker.this.token());
				TXImpl key = scanTXInPools(TXUtils.TXImpl(request.code(), request.version()));
				if (null == key)
					throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key
							+ "] not fould in registered txes: [" + METHOD_POOL.keySet().toString() + "].");

				Method method = METHOD_POOL.get(key);
				Object bean = INSTANCE_POOL.get(key);
				Object[] args = request.arguments();
				return resp.result(method.invoke(bean, args));
			} catch (Exception ex) {
				return resp.error(new Error(ExceptionUtils.unwrap(ex), Context.debug()));
			}
		}
	}

	private TXImpl scanTXInPools(TXImpl requestTX) {
		if (!TX_POOL.containsKey(requestTX.value())) return null;
		if (TX.ALL_VERSION.equals(requestTX)) return TX_POOL.get(requestTX.value()).first();
		return TX_POOL.get(requestTX.value()).ceiling(requestTX);
	}
}
