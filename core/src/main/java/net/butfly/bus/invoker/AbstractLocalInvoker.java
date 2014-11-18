package net.butfly.bus.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
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
	public Response invoke(Request request) throws Signal {
		if (this.auth != null) this.auth.login(this.token());
		return singleInvoke(request);
//		if (!(options instanceof AsyncRequest)) return singleInvoke(options);
//		AsyncRequest areq = (AsyncRequest) options;
//		if (!areq.continuous()) return singleInvoke(areq);
//		this.continuousInvoke(areq);
//		throw new IllegalAccessError("A continuous invoking should not end without exception.");
	}

//	private void continuousInvoke(AsyncRequest areq) {
//		try {
//			HandledBySignal.handleBySignal(new HandledBySignal(areq) {
//				@Override
//				public boolean retry() {
//					return areq.retry();
//				}
//
//				@Override
//				public void handle() throws Throwable {
//					areq.callback().callback(AbstractLocalInvoker.this.singleInvoke(areq));
//				}
//			});
//		} catch (SystemException e) {
//			throw e;
//		} catch (Throwable e) {
//			throw new SystemException("", e);
//		}
//	}

	protected Response singleInvoke(Request request) throws Signal {
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
			else throw new Signal.Completed(cause);
//			if (cause instanceof SystemException) throw (SystemException) cause;
//
//			String message = cause.getMessage();
//			if (message == null)
//				message = "Invoking method [" + clazz.getName() + "." + method.getName() + "] failure: Internal error.";
//			String errorCode = Constants.BusinessError.INVOKE_ERROR;
//			try {
//				Method m = cause.getClass().getMethod("getCode");
//				if (m != null) errorCode = m.invoke(cause).toString();
//			} catch (Exception ex) {}
//			throw new SystemException(errorCode, message, cause);
		}
	}

	private TXImpl scanTXInPools(TXImpl requestTX) {
		if (!TX_POOL.containsKey(requestTX.value())) return null;
		if (TX.ALL_VERSION.equals(requestTX)) return TX_POOL.get(requestTX.value()).first();
		return TX_POOL.get(requestTX.value()).ceiling(requestTX);
	}
}
