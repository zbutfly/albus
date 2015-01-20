package net.butfly.bus.invoker;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.service.AuthService;

public interface Invoker<C extends InvokerConfigBean> {

	void initialize(C config, Token token);

	Task.Callable<Response> task(final Request request, final Options... remoteOptions);

	Object[] getBeanList();

	String[] getTXCodes();

	AuthService authBean();

	Token token();

	Options localOptions(Options... options);

	Options[] remoteOptions(Options... options);
}
