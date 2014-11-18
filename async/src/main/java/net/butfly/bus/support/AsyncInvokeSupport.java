package net.butfly.bus.support;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
import net.butfly.bus.Request;
import net.butfly.bus.TX;

public interface AsyncInvokeSupport extends InvokeSupport {
	public <T, F extends Facade> F getService(Class<F> facadeClass, Callback<T> callback, Options options) throws Signal;

	public <T> void invoke(Request request, Callback<T> callback, Options options) throws Signal;

	public <T> void invoke(String code, Object[] arguments, Callback<T> callback, Options options) throws Signal;

	public <T> void invoke(TX tx, Object[] arguments, Callback<T> callback, Options options) throws Signal;
}
