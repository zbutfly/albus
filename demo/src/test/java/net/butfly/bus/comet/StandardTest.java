package net.butfly.bus.comet;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Token;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;
import net.butfly.bus.context.Context;
import net.butfly.bus.test.BusTest;

public class StandardTest extends BusTest {
	protected CometFacade facade;

	protected StandardTest(Mode mode) throws Exception {
		super(mode);
		Context.token(new Token("user", "pass"));
		Context.sourceAppID("CometTestClient");
		this.facade = this.client.service(CometFacade.class, new Options().fork());
	}

	public static void main(String args[]) throws Exception {
		run();
		waiting();
		// finish();
	}

	@Override
	protected void doAllTest() throws BusinessException {
		singletest();
		for (int i = 0; i < 5; i++)
			composetest(i);
	}

	@Override
	protected String[] getClientConfiguration() {
		return new String[] { "bus-comet-client.xml" };
	}

	@Override
	protected String[] getServerConfiguration() {
		return new String[] { "bus-comet-server.xml" };
	}

	/*****************************************************/

	private void singletest() {
		String str = facade.echoString("hello, world!");
		if (str != null) System.out.println("ECHO: " + str.toString());
		else System.err.println("do be null.");
	}

	protected void composetest(int times) {
		CometEchoReponse resp;
		resp = facade.echoCompose("hello, world!", Math.round(Math.random() * 100), Math.round(Math.random() * 100),
				Math.round(Math.random() * 100));
		if (resp != null) System.out.println("ECHO: " + resp.toString());
		else System.err.println("Do be null.");

		CometEchoRequest req = new CometEchoRequest();
		req.setValue("hello, world: [" + times + "]!");
		long[] values = new long[times];
		for (int i = 0; i < times; i++)
			values[i] = Math.round(Math.random() * 100) * i;
		req.setValues(values);
		resp = facade.echoObject(req);
		if (resp != null) System.out.println("ECHO: " + resp.toString());
		else System.err.println("Do be null.");
	}
}
