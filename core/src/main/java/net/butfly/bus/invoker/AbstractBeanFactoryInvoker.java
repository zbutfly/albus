package net.butfly.bus.invoker;

<<<<<<< HEAD
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.context.Token;
=======
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.InvokerConfig;
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491

public abstract class AbstractBeanFactoryInvoker extends AbstractLocalInvoker {
	@Override
	public final void initialize(InvokerConfig config, Token token) {
		super.initialize(config, token);
	}

	@Override
	public abstract Object[] getBeanList();
}
