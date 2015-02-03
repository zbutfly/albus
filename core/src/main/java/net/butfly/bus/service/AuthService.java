package net.butfly.bus.service;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.service.Service;
import net.butfly.bus.Token;

@AwareService
public interface AuthService extends Service {
	void logout() throws BusinessException;

	void login(Token token) throws BusinessException;
}
