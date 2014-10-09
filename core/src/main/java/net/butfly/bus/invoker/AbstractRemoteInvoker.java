package net.butfly.bus.invoker;

import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.ext.AsyncRequest;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	public AbstractRemoteInvoker() {
		this.continuousSupported = false;
	}

	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}

	@Override
	public Response invoke(Request request) {
		if (!(request instanceof AsyncRequest)) return singleInvoke(request);
		AsyncRequest areq = (AsyncRequest) request;
		if (!areq.continuous()) return singleInvoke(areq.request(this.token));
		this.continuousInvoke(areq);
		throw new IllegalAccessError("A continuous invoking should not end without exception.");
	}

	protected abstract void continuousInvoke(AsyncRequest areq);

	protected abstract Response singleInvoke(Request request);
}
