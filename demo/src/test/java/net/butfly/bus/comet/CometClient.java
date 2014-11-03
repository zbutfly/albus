package net.butfly.bus.comet;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.BasicBus;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.RepeatBus;
import net.butfly.bus.auth.Token;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.comet.facade.dto.CometEchoRequest;
import net.butfly.bus.context.Context;

public class CometClient {
	public static void main(String args[]) throws Exception {
		CometClient app = new CometClient();
		app.normal();
	}

	private BasicBus client;

	void normal() {
		this.client = new BasicBus("bus-comet-client.xml");
		Context.sourceAppID("CometClientTest");
		Context.token(new Token("user", "pass"));
		CometFacade comet = client.getService(CometFacade.class);
		for (int i = 0; i < 5; i++)
			this.singletest(comet, i);
	}

	void callback() {
		CallbackBus client = new CallbackBus("bus-comet-client.xml");
		CometFacade comet = client.getService(CometFacade.class, callback);
		for (int i = 0; i < 5; i++)
			this.singletest(comet, i);
		while (true)
			CometContext.sleep(10000);
	}

	void continuous() {
		RepeatBus client = new RepeatBus("bus-comet-client.xml");
		CometFacade comet = client.getService(CometFacade.class, callback, 0, 0);
		CometEchoReponse echo = comet.echo0("hello, world!");
		if (echo != null) System.err.println("Should be null: " + echo.toString());
		else System.out.println("Do be null.");
		while (true)
			CometContext.sleep(10000);
	}

	private final AsyncCallback<CometEchoReponse> callback = new AsyncCallback<CometEchoReponse>() {
		@Override
		public void callback(CometEchoReponse echo) {
			// consume one result.
			if (echo != null) System.out.println("Continuable echo: " + echo.toString());
		}
	};

	void singletest(CometFacade comet, int time) {
		CometEchoReponse resp;

		resp = comet.echo0("hello, world!");
		if (resp != null) System.out.println("ECHO: " + resp.toString());
		else System.err.println("do be null.");

		resp = comet.echo1("hello, world!", Math.round(Math.random() * 100), Math.round(Math.random() * 100),
				Math.round(Math.random() * 100));
		if (resp != null) System.out.println("ECHO: " + resp.toString());
		else System.err.println("Do be null.");

		CometEchoRequest req = new CometEchoRequest();
		req.setValue("hello, world: [" + time + "]!");
		long[] values = new long[time];
		for (int i = 0; i < time; i++)
			values[i] = Math.round(Math.random() * 100) * i;
		req.setValues(values);
		resp = comet.echo2(req);
		if (resp != null) System.out.println("ECHO: " + resp.toString());
		else System.err.println("Do be null.");
	}
}
