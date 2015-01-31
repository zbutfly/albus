package net.butfly.bus.invoker;

import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.base.Unit;
import net.butfly.albacore.utils.Keys;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.InvokerConfig;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class SpringInvoker extends AbstractLocalInvoker implements Invoker {
	private GenericXmlApplicationContext spring;

	@Override
	public void initialize(InvokerConfig config, Token token) {
		String module = config.param("module");
		String[] files = config.param("files").split(";");
		List<Resource> reses = new ArrayList<Resource>();
		Resource res;
		for (String file : files) {
			res = new ClassPathResource(file);
			if (res.exists()) reses.add(res);
		}
		spring = new GenericXmlApplicationContext();
		spring.load(reses.toArray(new Resource[reses.size()]));
		if (null != module) {
			PropertyPlaceholderConfigurer bean = new PropertyPlaceholderConfigurer();
			bean.setOrder(99);
			bean.setIgnoreResourceNotFound(true);
			reses = new ArrayList<Resource>();
			res = new ClassPathResource(module + "-internal.properties");
			if (res.exists()) reses.add(res);
			res = new ClassPathResource(module + ".properties");
			if (res.exists()) reses.add(res);
			bean.setLocations(reses.toArray(new Resource[reses.size()]));
			spring.getBeanFactory().registerSingleton(Keys.defaults(), bean);
		}
		spring.refresh();
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
