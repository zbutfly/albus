package net.butfly.bus.ext;

import java.io.Serializable;
import java.util.Map;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;

public interface TimeoutClientFacade {
	public <T, F extends Facade> F getService(Class<F> facadeClass, AsyncCallback<T> callback, long timeout);

	public <T, F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context,
			AsyncCallback<T> callback, long timeout);

	public void invoke(Request request, AsyncCallback<Response> callback, long timeout);

	public <T> void invoke(String code, AsyncCallback<T> callback, long timeout, Object... args);

	public <T> void invoke(TX tx, AsyncCallback<T> callback, long timeout, Object... args);
}
