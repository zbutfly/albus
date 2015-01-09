package net.butfly.bus.support;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.TX;

public interface InvokeExSupport extends InvokeSupport {
	public <T, F extends Facade> F getService(Class<F> facadeClass, Task.Callback<T> callback, Options... options) throws Signal;

	public <T> void invoke(Request request, Task.Callback<T> callback, Options... options) throws Signal;

	public <T> void invoke(String code, Object[] arguments, Task.Callback<T> callback, Options... options) throws Signal;

	public <T> void invoke(TX tx, Object[] arguments, Task.Callback<T> callback, Options... options) throws Signal;
}
