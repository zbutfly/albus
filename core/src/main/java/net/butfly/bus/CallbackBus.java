package net.butfly.bus;

import java.util.function.Consumer;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.policy.Routeable;

/**
 * <b>For Client-Side Async Callback Mode Only</b>.
 * 
 * @author butfly
 */
public interface CallbackBus extends Routeable {
	public <T, F extends Facade> F service(Class<F> facadeClass, Consumer<T> callback, Options... options)
			throws Exception;

	public <T> void invoke(String code, Object[] arguments, Consumer<T> callback, Options... options) throws Exception;

	public <T> void invoke(TX tx, Object[] arguments, Consumer<T> callback, Options... options) throws Exception;
}
