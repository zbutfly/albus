package net.butfly.bus.comet.facade;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.TX;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;

public interface CometFacade extends Facade {
	@TX("TST_CMT-000")
	public CometEchoReponse echo(String echo);

	@TX("TST_CMT-001")
	public CometEchoReponse continuableEcho(String echo);

	@TX("TST_CMT-002")
	public CometEchoReponse echo2(String echo, long[] values);

	@TX("TST_CMT-003")
	public CometEchoReponse echo3(CometEchoRequest echo);
}
