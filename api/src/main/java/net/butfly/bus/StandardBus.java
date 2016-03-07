package net.butfly.bus;

import net.butfly.albacore.facade.Facade;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.policy.Routeable;

public interface StandardBus extends Routeable {
	public <T, F extends Facade> F service(Class<F> facadeClass, Options... options) throws Exception;

	public <T> T invoke(String code, Object[] args, Options... options) throws Exception;

	public <T> T invoke(TX tx, Object[] args, Options... options) throws Exception;
}
