package net.butfly.bus.service;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.service.Service;
<<<<<<< HEAD
import net.butfly.bus.context.Token;
=======
import net.butfly.bus.Token;
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491

@AwareService
public interface AuthService extends Service {
	void logout() throws BusinessException;

	void login(Token token) throws BusinessException;
}
