package net.butfly.bus;

import java.io.Serializable;
import java.util.Map;

import net.butfly.albacore.facade.Facade;

public interface ClientFacade {
	public Response invoke(Request request);

	public <F extends Facade> F getService(Class<F> facadeClass);

	public <F extends Facade> F getService(Class<F> facadeClass, Map<String, Serializable> context);

	public <T> T invoke(String code, Object... args);

	public <T> T invoke(TX tx, Object... args);
}
