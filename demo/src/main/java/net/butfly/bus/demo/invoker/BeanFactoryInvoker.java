package net.butfly.bus.demo.invoker;

import net.butfly.bus.comet.facade.CometFacadeImpl;
import net.butfly.bus.invoker.AbstractBeanFactoryInvoker;


public class BeanFactoryInvoker extends AbstractBeanFactoryInvoker {
	@Override
	public Object[] getBeanList() {
		return new Object[] { new CometFacadeImpl() };
	}
}
