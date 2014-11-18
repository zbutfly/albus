package net.butfly.bus.support;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;

public interface InvokeSupport {
	public <F extends Facade> F getService(Class<F> facadeClass);

	public Response invoke(Request request);

	public <T> T invoke(String code, Object... args);

	public <T> T invoke(TX tx, Object... args);
}
