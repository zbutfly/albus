package net.butfly.bus.comet;

import net.butfly.albacore.utils.async.Callback;
import net.butfly.bus.ext.Bus;
import net.butfly.bus.auth.Token;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;
import net.butfly.bus.context.Context;
import net.butfly.bus.utils.async.Options;

public class CometClient {
	private Bus client;
	private CometFacade facade;

	public CometClient(boolean remote) {
		Context.initialize(null, false);
		Context.sourceAppID("CometClientTest");
		this.client = new Bus(remote ? "bus-comet-client.xml" : "bus-comet-server.xml");
		this.facade = this.client.getService(CometFacade.class);
	}

	public static void main(String args[]) throws Exception {
		CometClient app = new CometClient(false);
		Context.token(new Token("user", "pass"));
		for (int i = 0; i < 5; i++)
			app.singletest(i);
	}

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
