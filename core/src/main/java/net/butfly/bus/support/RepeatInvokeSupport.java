package net.butfly.bus.support;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.TX;

public interface RepeatInvokeSupport extends TimeoutInvokeSupport {
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback, long timeout, int retries);

	public void invoke(Request request, AsyncCallback<Response> callback, long timeout, int retries);

	public <T> void invoke(String code, AsyncCallback<T> callback, long timeout, int retries, Object... args);

	public <T> void invoke(TX tx, AsyncCallback<T> callback, long timeout, int retries, Object... args);
}
