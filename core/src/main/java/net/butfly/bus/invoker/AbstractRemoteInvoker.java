package net.butfly.bus.invoker;

import java.io.IOException;
import java.util.Arrays;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	protected Options[] remoteOptions(Options... options) {
		if (options == null || options.length == 0) return null;
		if (options.length == 1) return options;
		return Arrays.copyOfRange(options, 1, options.length - 1);
	}

	@Override
	protected Options localOptions(Options... options) {
		return options == null || options.length <= 1 ? new Options() : options[0];
	}

	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}

	@Override
	public final Response invoke(final Request request, final Options... options) throws Exception {
		request.token(this.token());
		return this.invokeRemote(request, options);
	}

	@Override
	public void invoke(final Request request, final Task.Callback<Response> callback, final Options... options)
			throws IOException {
		request.token(this.token());
		this.invokeRemote(request, callback, options);
	}

	protected abstract Response invokeRemote(Request request, Options... options) throws IOException;

	protected abstract void invokeRemote(final Request request, final Task.Callback<Response> callback,
			final Options... options) throws IOException;
}
