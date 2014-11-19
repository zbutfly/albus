package net.butfly.bus.invoker;

import java.io.IOException;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}

	@Override
	public Response invoke(Request request) throws Signal {
		try {
			request.token(this.token());
			return this.singleInvoke(request);
			// if (!(options instanceof AsyncRequest))
			// return this.singleInvoke(request);
			// AsyncRequest areq = (AsyncRequest) options;
			// if (!areq.continuous()) return syncInvoke(areq);
			// this.asyncInvoke(areq);
			// throw new
			// IllegalAccessError("A continuous invoking should not end without exception.");
		} catch (IOException ex) {
			throw new SystemException("", ex);
		}
	}
}
