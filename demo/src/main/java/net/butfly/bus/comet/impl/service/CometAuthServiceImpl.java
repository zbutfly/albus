package net.butfly.bus.comet.impl.service;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.auth.Token;
import net.butfly.bus.comet.service.CometAuthService;
import net.butfly.bus.context.Context;
import net.butfly.bus.service.AuthServiceImpl;

public class CometAuthServiceImpl extends AuthServiceImpl implements CometAuthService {
	private static final long serialVersionUID = -2966188033054529243L;

	@Override
	public void login(Token token) {
		if (null == token) throw new SystemException(Constants.BusinessError.AUTH_NOT_EXIST, "Authorization lost.");
		if (null != token.getKey()) {
			if (!"token".equals(token.getKey()))
				throw new SystemException(Constants.BusinessError.AUTH_TOKEN_INVALID,
						"Authorization failure for invalid token.");;
			Context.token(token);
			return;
		}
		if (null != token.getUsername() && null != token.getPassword()) {
			if (!"user".equals(token.getUsername()) || !"pass".equals(token.getPassword()))
				throw new SystemException(Constants.BusinessError.AUTH_PASS_INVALID,
						"Authorization failure for invalid username/password.");
			Context.token(new Token("token"));
			return;
		}
		throw new SystemException(Constants.BusinessError.AUTH_TOKEN_INVALID, "Authorization failure for no token provided.");
	}
}
