package net.butfly.bus.invoker;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;

import net.butfly.albacore.base.ModuleApplicationContext;
import net.butfly.albacore.base.Unit;
import net.butfly.albacore.utils.Springs;
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.context.Token;

public class SpringInvoker extends AbstractLocalInvoker implements Invoker {
	private ApplicationContext spring;

	@Override
	public void initialize(InvokerConfig config, Token token) {
		spring = new ModuleApplicationContext(config.param("module", "albus"), Springs.searchResource(config.param("files").split(";")));
		super.initialize(config, token);
	}

	@Override
	public Object[] getBeanList() {
		List<Object> beans = new ArrayList<Object>();
		for (String name : spring.getBeanDefinitionNames())
			if (Unit.class.isAssignableFrom(spring.getType(name)) && !((BeanDefinitionRegistry) spring).getBeanDefinition(name)
					.isAbstract()) beans.add(spring.getBean(name));
		return beans.toArray(new Object[beans.size()]);
	}
}
