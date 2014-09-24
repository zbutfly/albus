package net.butfly.bus.comet.facade;

import net.butfly.albacore.facade.FacadeBase;
import net.butfly.bus.comet.CometContext;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;
import net.butfly.bus.util.async.Signal;

public class CometFacadeImpl extends FacadeBase implements CometFacade {
	private static final long serialVersionUID = 4279578356782869791L;
	private long count = 0;

	@Override
	public CometEchoReponse echo(String echo) {
		return new CometEchoReponse(echo + " [from echo()]");
	}

	@Override
	public CometEchoReponse continuableEcho(String echo) {
		CometContext.sleep(500);
		long c = ++count;
		if (c % 5 == 0) throw new Signal.Suspend(7000);
		if (c % 8 == 0) throw new Signal.Timeout();
		if (c % 30 == 0) throw new Signal.Completed();
		return new CometEchoReponse("[#" + c + "] " + echo + " [from continuableEcho()]");
	}

	@Override
	public CometEchoReponse echo2(String echo, long[] values) {
		return new CometEchoReponse(echo + " [from echo2()]");
	}

	@Override
	public CometEchoReponse echo3(CometEchoRequest echo) {
		return new CometEchoReponse(echo.getValue() + " [from echo3()]");
	}
}
