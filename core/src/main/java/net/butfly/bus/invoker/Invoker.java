package net.butfly.bus.invoker;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;

public interface Invoker<C extends InvokerConfigBean> {
	public enum Mode {
		SERVER, CLIENT;
	};

	void initialize(C config, Token token);

	Response invoke(final Request req, final Options... options) throws Exception;

	void invoke(final Request request, final Task.Callback<Response> callback, final Options... options) throws Exception;

	Object[] getBeanList();

	String[] getTXCodes();

	Mode mode();

}
