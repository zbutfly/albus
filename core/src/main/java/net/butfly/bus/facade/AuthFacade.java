package net.butfly.bus.facade;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.TX;
import net.butfly.bus.auth.Token;

public interface AuthFacade extends Facade {
	@TX("BUS_AUTH-000")
	void logout();

	@TX("BUS_AUTH-001")
	void login(Token token);
}
