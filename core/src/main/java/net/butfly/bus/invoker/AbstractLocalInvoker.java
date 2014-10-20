package net.butfly.bus.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Constants;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.context.Context;
import net.butfly.bus.ext.AsyncRequest;
import net.butfly.bus.util.TXUtils;
import net.butfly.bus.util.TXUtils.TXImpl;
import net.butfly.bus.util.async.AsyncInvokeUtils;
import net.butfly.bus.util.async.HandledBySignal;
import net.butfly.bus.util.async.Signal;

public abstract class AbstractLocalInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	public Method getMethod(String code, String version) {
		TXImpl key = this.scanTXInPools(TXUtils.TXImpl(code, version));
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");
		return METHOD_POOL.get(key);
	}

	@Override
	public Response invoke(Request request) {
		if (this.auth != null) {
			Token t = Context.token();
			if (null == t) t = this.token;
			this.auth.login(t);
		}
		if (!(request instanceof AsyncRequest)) return singleInvoke(request);
		AsyncRequest areq = (AsyncRequest) request;
		if (!areq.continuous()) return singleInvoke(areq.request(this.token));
		this.continuousInvoke(areq);
		throw new IllegalAccessError("A continuous invoking should not end without exception.");
	}

	private void continuousInvoke(AsyncRequest areq) {
		try {
			AsyncInvokeUtils.handleBySignal(new HandledBySignal(areq) {
				@Override
				public boolean retry() {
					return areq.retry();
				}

				@Override
				public void handle() throws Throwable {
					areq.callback().callback(
							AbstractLocalInvoker.this.singleInvoke(areq.request(AbstractLocalInvoker.this.token)));
				}
			});
		} catch (SystemException e) {
			throw e;
		} catch (Throwable e) {
			throw new SystemException("", e);
		}
	}

	private Response singleInvoke(Request request) {
		TXImpl key = this.scanTXInPools(TXUtils.TXImpl(request.code(), request.version()));
		if (null == key)
			throw new SystemException(Constants.BusinessError.CONFIG_ERROR, "TX [" + key + "] not fould in registered txes: ["
					+ METHOD_POOL.keySet().toString() + "].");

		Method method = METHOD_POOL.get(key);
		Object bean = INSTANCE_POOL.get(key);
		Object[] args = request.arguments();
		Class<?> clazz = null == bean ? method.getDeclaringClass() : bean.getClass();
		try {
			return new Response(request).result(method.invoke(bean, args));
		} catch (IllegalArgumentException e) {
			throw new SystemException(Constants.BusinessError.INVOKE_ERROR, "Invoking method [" + clazz.getName() + "."
					+ method.getName() + "] failure: Arguments mismatch.", e);
		} catch (IllegalAccessException e) {
			throw new SystemException(Constants.BusinessError.INVOKE_ERROR, "Invoking method [" + clazz.getName() + "."
					+ method.getName() + "] failure: Not public", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getTargetException();
			if (cause instanceof Signal) throw (Signal) cause;
			if (cause instanceof SystemException) throw (SystemException) cause;

			String message = cause.getMessage();
			if (message == null)
				message = "Invoking method [" + clazz.getName() + "." + method.getName() + "] failure: Internal error.";
			String errorCode = Constants.BusinessError.INVOKE_ERROR;
			try {
				Method m = cause.getClass().getMethod("getCode");
				if (m != null) errorCode = m.invoke(cause).toString();
			} catch (Exception ex) {}
			throw new SystemException(errorCode, message, cause);
		}

	}

	private TXImpl scanTXInPools(TXImpl requestTX) {
		if (!TX_POOL.containsKey(requestTX.value())) return null;
		if (TX.ALL_VERSION.equals(requestTX)) return TX_POOL.get(requestTX.value()).first();
		return TX_POOL.get(requestTX.value()).ceiling(requestTX);
	}
}
