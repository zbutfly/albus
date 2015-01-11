package net.butfly.bus;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;

public interface Bus {
	// sync invoking
	public <T, F extends Facade> F getService(Class<F> facadeClass, Options... options) throws Exception;

	public <T> T invoke(String code, Object[] args, Options... options) throws Exception;

	public <T> T invoke(TX tx, Object[] args, Options... options) throws Exception;

	// async invoking
	public <T, F extends Facade> F getService(Class<F> facadeClass, Task.Callback<T> callback, Options... options)
			throws Exception;

	public <T> void invoke(String code, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception;

	public <T> void invoke(TX tx, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception;

}
