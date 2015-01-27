package net.butfly.bus.invoker;

import net.butfly.albacore.service.Service;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public interface Invoker<C extends InvokerConfigBean> {
	void initialize(C config, Token token);

	void initialize();

	Response invoke(Request request, Options... remoteOptions) throws Exception;

	Object[] getBeanList();

	String[] getTXCodes();

	<S extends Service> S awared(Class<S> serviceClass);

	Token token();

	Options localOptions(Options... options);

	Options[] remoteOptions(Options... options);
}
