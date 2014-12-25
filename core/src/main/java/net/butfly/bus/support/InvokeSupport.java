package net.butfly.bus.support;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;

public interface InvokeSupport {
	public <F extends Facade> F getService(Class<F> facadeClass, Options... options) throws Signal;

	public Response invoke(Request request, Options... options) throws Signal;

	public <T> T invoke(String code, Object[] args, Options... options) throws Signal;

	public <T> T invoke(TX tx, Object[] args, Options... options) throws Signal;
}
