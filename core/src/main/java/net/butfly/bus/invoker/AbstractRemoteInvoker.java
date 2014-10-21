package net.butfly.bus.invoker;

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
		if (!(request instanceof AsyncRequest)) return singleInvoke(request);
		AsyncRequest areq = (AsyncRequest) request;
		if (!areq.continuous()) return singleInvoke(areq.request(this.token));
		this.continuousInvoke(areq);
		throw new IllegalAccessError("A continuous invoking should not end without exception.");
	}

	protected abstract void continuousInvoke(AsyncRequest areq);

	protected abstract Response singleInvoke(Request request);
}
