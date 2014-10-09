package net.butfly.bus.invoker;

import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public interface Invoker<C extends InvokerConfigBean> {
	void initialize(C config, Token token);

	Response invoke(Request request);

	Object[] getBeanList();

	String[] getTXCodes();

	boolean continuousSupported();
}
