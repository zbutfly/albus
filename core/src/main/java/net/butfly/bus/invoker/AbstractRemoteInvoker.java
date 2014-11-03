package net.butfly.bus.invoker;

import java.io.IOException;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.argument.AsyncRequest;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}

	@Override
	public Response invoke(Request request) {
		try {
			request.token(this.token());
			if (!(request instanceof AsyncRequest)) return syncInvoke(request);
			AsyncRequest areq = (AsyncRequest) request;
			if (!areq.continuous()) return syncInvoke(areq);
			this.asyncInvoke(areq);
			throw new IllegalAccessError("A continuous invoking should not end without exception.");
		} catch (IOException ex) {
			throw new SystemException("", ex);
		}
	}

	protected abstract void asyncInvoke(AsyncRequest areq) throws IOException;

	protected abstract Response syncInvoke(Request request) throws IOException;
}
