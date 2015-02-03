package net.butfly.bus.invoker;

import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.base.Unit;
import net.butfly.albacore.utils.Springs;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.InvokerConfig;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.GenericXmlApplicationContext;

public class SpringInvoker extends AbstractLocalInvoker implements Invoker {
	private GenericXmlApplicationContext spring;

	@Override
	public void initialize(InvokerConfig config, Token token) {
		String module = config.param("module");
		spring = new GenericXmlApplicationContext();
		spring.load(Springs.searchResource(config.param("files").split(";")));
		this.append(spring.getBeanFactory(), module);
		spring.refresh();
		super.initialize(config, token);
	}

	private void append(ConfigurableListableBeanFactory beanFactory, String module) {
		if (null == module) return;
		while (module.startsWith("/"))
			module = module.substring(1);
		while (module.endsWith("/"))
			module = module.substring(0, module.length() - 1);
		Springs.appendPlaceholder(beanFactory, 99,
				Springs.searchResource(module + "-internal.properties", module + ".properties"),
				"albacore.mybatis.config.location.pattern", "classpath*:**/" + module + "-mybatis-config.xml");
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
