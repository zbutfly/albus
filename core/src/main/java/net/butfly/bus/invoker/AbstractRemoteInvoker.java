package net.butfly.bus.invoker;

import java.io.IOException;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}

	public void invoke(final Request request, final Task.Callback<Response> callback, final Options options) throws IOException {
		request.token(this.token());
		this.invokeRemote(request, callback, options);
	}

	@Override
	public final Response invoke(final Request request, final Options options) throws IOException {
		request.token(this.token());
		return this.invokeRemote(request, options);
	}

	protected abstract Response invokeRemote(final Request request, final Options options) throws IOException;

	protected abstract void invokeRemote(final Request request, final Task.Callback<Response> callback, final Options options)
			throws IOException;
}
