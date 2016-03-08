package net.butfly.bus.service;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.service.ServiceBase;
import net.butfly.bus.context.Context;

public abstract class AuthServiceImpl extends ServiceBase implements AuthService {
	private static final long serialVersionUID = 1L;

	@Override
	public void logout() throws BusinessException {
		Context.untoken();
	}
}
