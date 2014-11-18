package net.butfly.bus.invoker;

import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public interface Invoker<C extends InvokerConfigBean> {
	void initialize(C config, Token token);

	Response invoke(Request request) throws Signal;

	Object[] getBeanList();

	String[] getTXCodes();
}
