package net.butfly.bus.support;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;

public interface InvokeSupport {
	public <F extends Facade> F getService(Class<F> facadeClass) throws Signal;

	public Response invoke(Request request) throws Signal;

	public <T> T invoke(String code, Object... args) throws Signal;

	public <T> T invoke(TX tx, Object... args) throws Signal;
}
