package net.butfly.bus.invoker;

import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractBeanFactoryInvoker extends AbstractLocalInvoker<InvokerConfigBean> {
	@Override
	public final void initialize(InvokerConfigBean config) {
		super.initialize(config);
	}

	@Override
	public abstract Object[] getBeanList();
}
