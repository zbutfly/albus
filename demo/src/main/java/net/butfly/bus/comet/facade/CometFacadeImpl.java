package net.butfly.bus.comet.facade;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.facade.FacadeBase;
import net.butfly.bus.argument.Constants;
import net.butfly.bus.auth.Token;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;
import net.butfly.bus.context.Context;
import net.butfly.bus.facade.AuthFacade;

public class CometFacadeImpl extends FacadeBase implements CometFacade, AuthFacade {
	private static final long serialVersionUID = 4279578356782869791L;
	private long count = 0;

	private synchronized long count() {
		long c = ++count;
		// if (c % 3 == 0) CometContext.sleep(200);
		// if (c % 5 == 0) throw new Signal.Suspend(7000);
		// if (c % 8 == 0) throw new Signal.Timeout();
		// if (c % 30 == 0) throw new Signal.Completed();
		return c;
	}

	@Override
	public String echoString(String echo) {
		return echo + " [from echo0][" + count() + "]";
	}

	@Override
	public long echoArray(long... values) {
		return values[(int) Math.floor(Math.random() * values.length)];
	}

	@Override
	public CometEchoReponse echoCompose(String echo, long... values) {
		return new CometEchoReponse(echo + " [from echo1][" + count() + "]", values);
	}

	@Override
	public CometEchoReponse echoObject(CometEchoRequest echo) {
		return new CometEchoReponse(echo.getValue() + " [from echo2][" + count() + "]", echo.getValues());
	}

	@Override
	public void logout() {
		Context.untoken();
	}

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
