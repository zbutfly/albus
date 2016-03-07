package net.butfly.bus.invoker;

import net.butfly.albacore.service.Service;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
<<<<<<< HEAD
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.context.Token;
=======
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.InvokerConfig;
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
import net.butfly.bus.policy.Routeable;

public interface Invoker extends Routeable {
	void initialize(InvokerConfig config, Token token);

	void initialize();

	Response invoke(Request request, Options... remoteOptions) throws Exception;

	Object[] getBeanList();

	<S extends Service> S awared(Class<S> awaredServiceClass);

	Token token();

	Options localOptions(Options... options);

	Options[] remoteOptions(Options... options);
	
	boolean lazy();
}
