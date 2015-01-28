package net.butfly.bus.invoker;

import java.util.Arrays;

import net.butfly.albacore.service.Service;
import net.butfly.albacore.utils.async.Options;

public abstract class AbstractRemoteInvoker extends AbstractInvoker {
	@Override
	public final Options[] remoteOptions(Options... options) {
		if (options == null || options.length == 0) return null;
		if (options.length == 1) return options;
		return Arrays.copyOfRange(options, 1, options.length - 1);
	}

	@Override
	public final Options localOptions(Options... options) {
		return options == null || options.length <= 1 ? new Options() : options[0];
	}

	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}

	@Override
	public boolean isSupported(String tx) {
		return config.isSupported(tx);
	}

	@Override
	public <S extends Service> S awared(Class<S> serviceClass) {
		return null;
	}

	@Override
	public boolean lazy() {
		String l = System.getProperty("bus.invoker.spring.lazy");
		return Boolean.parseBoolean(l == null ? config.param("lazy") : l);
	}
}
