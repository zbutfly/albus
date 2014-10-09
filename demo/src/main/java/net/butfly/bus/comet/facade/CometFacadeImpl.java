package net.butfly.bus.comet.facade;

import net.butfly.albacore.facade.FacadeBase;
import net.butfly.bus.auth.Token;
import net.butfly.bus.comet.CometContext;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;
import net.butfly.bus.context.Context;
import net.butfly.bus.facade.AuthFacade;
import net.butfly.bus.util.async.Signal;

public class CometFacadeImpl extends FacadeBase implements CometFacade, AuthFacade {
	private static final long serialVersionUID = 4279578356782869791L;
	private long count = 0;

	@Override
	public String echo0(String echo) {
		return echo + " [from echo0()]";
	}

	@Override
	public CometEchoReponse echo1(String echo, long... values) {
		return new CometEchoReponse(echo + " [from echo1()]");
	}

	@Override
	public CometEchoReponse echo2(CometEchoRequest echo) {
		return new CometEchoReponse(echo.getValue() + " [from echo2()]");
	}

	@Override
	public String continuableEcho0(String echo) {
		CometContext.sleep(500);
		long c = ++count;
		if (c % 5 == 0) throw new Signal.Suspend(7000);
		if (c % 8 == 0) throw new Signal.Timeout();
		if (c % 30 == 0) throw new Signal.Completed();
		return "[#" + c + "] " + echo + " [from continuableEcho0()]";
	}

	@Override
	public CometEchoReponse continuableEcho1(String echo, long... values) {
		CometContext.sleep(500);
		long c = ++count;
		if (c % 5 == 0) throw new Signal.Suspend(7000);
		if (c % 8 == 0) throw new Signal.Timeout();
		if (c % 30 == 0) throw new Signal.Completed();
		return new CometEchoReponse("[#" + c + "] " + echo + " [from continuableEcho1()]");
	}

	@Override
	public void logout() {
		Context.untoken();
	}

	@Override
	public void login(Token token) {
		if (null == token) throw new IllegalAccessError("Authorization failure for no token provided.");
		if (null != token.getKey()) {
			if (!"token".equals(token.getKey())) throw new IllegalAccessError("Authorization failure for invalid token.");;
			Context.token(token);
			return;
		}
		if (null != token.getUsername() && null != token.getPassword()) {
			if (!"user".equals(token.getUsername()) || !"pass".equals(token.getPassword()))
				throw new IllegalAccessError("Authorization failure for invalid username/password.");
			Context.token(new Token("token"));
			return;
		}
		throw new IllegalAccessError("Authorization failure for no token provided.");
	}
}
