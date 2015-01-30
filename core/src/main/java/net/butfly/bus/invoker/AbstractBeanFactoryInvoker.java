package net.butfly.bus.invoker;

import net.butfly.bus.Token;
import net.butfly.bus.config.bean.InvokerConfig;

public abstract class AbstractBeanFactoryInvoker extends AbstractLocalInvoker {
	@Override
	public final void initialize(InvokerConfig config, Token token) {
		super.initialize(config, token);
	}

	@Override
	public abstract Object[] getBeanList();
}
