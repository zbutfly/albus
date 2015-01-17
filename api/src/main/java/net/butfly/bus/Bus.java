package net.butfly.bus;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.policy.Routeable;

public interface Bus extends Routeable {
	<T, F extends Facade> F service(Class<F> facadeClass, Options... options) throws Exception;

	<T> T invoke(String code, Object[] args, Options... options) throws Exception;

	<T> T invoke(TX tx, Object[] args, Options... options) throws Exception;

	<T, F extends Facade> F service(Class<F> facadeClass, Task.Callback<T> callback, Options... options)
			throws Exception;

	<T> void invoke(String code, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception;

	<T> void invoke(TX tx, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception;
}
