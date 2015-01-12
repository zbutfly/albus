package net.butfly.bus.invoker;

import java.util.Arrays;

import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	protected final Options[] remoteOptions(Options... options) {
		if (options == null || options.length == 0) return null;
		if (options.length == 1) return options;
		return Arrays.copyOfRange(options, 1, options.length - 1);
	}

	@Override
	protected final Options localOptions(Options... options) {
		return options == null || options.length <= 1 ? new Options() : options[0];
	}

	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}
}
