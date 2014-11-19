package net.butfly.bus.invoker;

import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.base.Unit;

import net.butfly.bus.config.invoker.SpringInvokerConfig;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class SpringInvoker extends AbstractLocalInvoker<SpringInvokerConfig> implements Invoker<SpringInvokerConfig> {
	private ApplicationContext spring;
	private String files;

	@Override
	public void initialize(SpringInvokerConfig config) {
		this.files = config.getFiles();
		super.initialize(config);
	}

	@Override
	public Object[] getBeanList() {
		String[] filelist = files.split(";");
		List<Resource> reses = new ArrayList<Resource>();
		for (String file : filelist) {
			reses.add(new ClassPathResource(file));
			logger.trace("Invoker [SPRING:" + file + "] parsing...");
		}
		this.spring = new GenericXmlApplicationContext(reses.toArray(new Resource[reses.size()]));

		List<Object> beans = new ArrayList<Object>();
		for (String name : spring.getBeanDefinitionNames())
			if (Unit.class.isAssignableFrom(spring.getType(name))
					&& !((BeanDefinitionRegistry) spring).getBeanDefinition(name).isAbstract())
				beans.add(spring.getBean(name));
		logger.trace("Invoker [SPRING] context parsed.");
		return beans.toArray(new Object[beans.size()]);
	}
}
