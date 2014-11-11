package net.butfly.bus.comet.facade;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.argument.TX;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;

public interface CometFacade extends Facade {
	@TX("TST_CMT-000")
	public String echoString(String echo);

	@TX("TST_CMT-001")
	public long echoArray(long... values);

	@TX("TST_CMT-002")
	public CometEchoReponse echoCompose(String echo, long... values);

	@TX("TST_CMT-003")
	public CometEchoReponse echoObject(CometEchoRequest echo);

}
