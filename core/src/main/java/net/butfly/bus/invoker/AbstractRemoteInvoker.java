package net.butfly.bus.invoker;

import java.io.IOException;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public abstract class AbstractRemoteInvoker<C extends InvokerConfigBean> extends AbstractInvoker<C> {
	@Override
	public Object[] getBeanList() {
		return new Object[0];
	}

	@Override
	public final Response invoke(final Request request, final Options options) throws Signal {
		request.token(this.token());
		try {
			return this.invokeRemote(request, options);
		} catch (IOException e) {
			throw new Signal.Completed(e);
		}
	}

	protected abstract Response invokeRemote(final Request request, final Options options) throws IOException, Signal;
}
