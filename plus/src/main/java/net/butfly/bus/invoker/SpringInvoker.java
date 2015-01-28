package net.butfly.bus.invoker;

import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.base.Unit;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.InvokerBean;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class SpringInvoker extends AbstractLocalInvoker implements Invoker {
	private ApplicationContext spring;

	@Override
	public void initialize(InvokerBean config, Token token) {
		String[] files = config.param("files").split(";");
		List<Resource> reses = new ArrayList<Resource>();
		for (String file : files)
			reses.add(new ClassPathResource(file));
		this.spring = new GenericXmlApplicationContext(reses.toArray(new Resource[reses.size()]));
		super.initialize(config, token);
	}

	@Override
	public Object[] getBeanList() {
		List<Object> beans = new ArrayList<Object>();
		for (String name : spring.getBeanDefinitionNames())
			if (Unit.class.isAssignableFrom(spring.getType(name))
					&& !((BeanDefinitionRegistry) spring).getBeanDefinition(name).isAbstract())
				beans.add(spring.getBean(name));
		return beans.toArray(new Object[beans.size()]);
	}
}
