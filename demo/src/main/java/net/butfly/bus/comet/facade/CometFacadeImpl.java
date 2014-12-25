package net.butfly.bus.comet.facade;

import net.butfly.albacore.facade.FacadeBase;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;

public class CometFacadeImpl extends FacadeBase implements CometFacade {
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
}
