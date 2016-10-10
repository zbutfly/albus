package net.butfly.bus.demo;

import net.butfly.bus.demo.facade.SampleFacadeImpl;
import net.butfly.bus.invoker.AbstractBeanFactoryInvoker;

public class BeanFactory extends AbstractBeanFactoryInvoker {
	@Override
	public Object[] getBeanList() {
		return new Object[] { new SampleFacadeImpl() };
	}
}
