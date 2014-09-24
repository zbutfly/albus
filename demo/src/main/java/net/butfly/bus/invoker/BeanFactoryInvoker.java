package net.butfly.bus.invoker;

import net.butfly.bus.comet.facade.CometFacadeImpl;


public class BeanFactoryInvoker extends AbstractBeanFactoryInvoker {
	@Override
	public Object[] getBeanList() {
		return new Object[] { new CometFacadeImpl() };
	}
}
