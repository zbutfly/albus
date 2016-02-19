package net.butfly.bus;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.policy.Routeable;

/**
 * <b>For Client-Side Async Callback Mode Only</b>.
 * 
 * @author butfly
 */
public interface CallbackBus extends Routeable {
	public <T, F> F service(Class<F> facadeClass, Task.Callback<T> callback, Options... options) throws Exception;

	public <T> void invoke(String code, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception;

	public <T> void invoke(TX tx, Object[] arguments, Task.Callback<T> callback, Options... options) throws Exception;
}
