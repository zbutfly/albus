package net.butfly.bus.comet;

import net.butfly.albacore.utils.async.Callback;
import net.butfly.bus.auth.Token;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;
import net.butfly.bus.context.Context;
import net.butfly.bus.deploy.JettyStarter;
import net.butfly.bus.ext.Bus;
import net.butfly.bus.test.BusTest;
import net.butfly.bus.utils.async.Options;

public class CometTest extends BusTest {
	private CometFacade facade;

	protected CometTest(boolean remote) throws Exception {
		super(remote);
	}

	public static void main(String args[]) throws Exception {
		run();
	}

	@Override
	protected void doAllTest() {
		for (int i = 0; i < 5; i++)
			singletest(i);
	}

	@Override
	protected String[] getClientConfiguration() {
		return new String[] { "bus-comet-client.xml" };
	}

	@Override
	protected String[] getServerConfiguration() {
		return new String[] { "bus-comet-server.xml" };
	}

	@Override
	protected void beforeBus(boolean remote) throws Exception {
		Context.token(new Token("user", "pass"));
		Context.sourceAppID("CometClientTest");
		if (remote) {
			JettyStarter.main("-h");
			System.setProperty("bus.server.class", "net.butfly.bus.ext.Bus");
		}
	}

	@Override
	protected void beforeTest() {
		this.facade = this.client.getService(CometFacade.class);
	}

	/*****************************************************/
	void callback() {
		for (int i = 0; i < 5; i++)
			this.singletest(i);
		while (true)
			CometContext.sleep(10000);
	}

	void continuous() {
		Bus client = new Bus("bus-comet-client.xml");
		CometFacade comet = client.getService(CometFacade.class, callback, new Options().retries(3));
		String echo = comet.echoString("hello, world!");
		if (echo != null) System.err.println("Should be null: " + echo.toString());
		else System.out.println("Do be null.");
		while (true)
			CometContext.sleep(10000);
	}

	private final Callback<CometEchoReponse> callback = new Callback<CometEchoReponse>() {
		@Override
		public void callback(CometEchoReponse echo) {
			// consume one result.
			if (echo != null) System.out.println("Continuable echo: " + echo.toString());
		}
	};

	void singletest(int times) {
		CometEchoReponse resp;

		String str = facade.echoString("hello, world!");
		if (str != null) System.out.println("ECHO: " + str.toString());
		else System.err.println("do be null.");

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
