package net.butfly.bus.invoker;

import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	public AbstractRemoteInvoker() {
		this.continuousSupported = true;
	}

	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}
}
