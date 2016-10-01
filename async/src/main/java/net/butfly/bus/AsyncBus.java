package net.butfly.bus;

import net.butfly.albacore.lambda.Consumer;
import net.butfly.albacore.utils.async.Options;

/**
 * <b>For Client-Side Async Callback Mode Only</b>.
 * 
 * @author butfly
 */
public interface AsyncBus extends Bus {
	public <T, F> F service(Class<F> facadeClass, Consumer<T> callback, Options... options) throws Exception;

	public <T> void invoke(String code, Object[] arguments, Consumer<T> callback, Options... options) throws Exception;

	public <T> void invoke(TX tx, Object[] arguments, Consumer<T> callback, Options... options) throws Exception;
}
