package net.butfly.bus.invoker;

import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public interface Invoker<C extends InvokerConfigBean> {
	void initialize(C config, Token token);

	Response invoke(final Request request, final Options options) throws Signal;

	void invoke(final Request request, final Callback<Response> callback, final Options options) throws Signal;

	Object[] getBeanList();

	String[] getTXCodes();
}
