package net.butfly.bus.comet;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.BasicBus;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.RepeatBus;
import net.butfly.bus.auth.Token;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.context.Context;

public class CometClient {
	public static void main(String args[]) throws Exception {
		CometClient app = new CometClient();
		app.normal();
	}

	void normal() {
		// final signal sig = new signal();
		BasicBus client = new BasicBus("bus-comet-client.xml");
		Context.sourceAppID("CometClientTest");
		Context.token(new Token("user", "pass"));
		CometFacade comet = client.getService(CometFacade.class);
		System.out.println("Sync echo: " + comet.echo0("hello, world!"));
		System.out.println("Sync echo: " + comet.echo0("hello, world!"));
		System.out.println("Sync echo: " + comet.echo0("hello, world!"));
		System.out.println("Sync echo: " + comet.echo0("hello, world!"));
	}

	private final AsyncCallback<CometEchoReponse> callback = new AsyncCallback<CometEchoReponse>() {
		@Override
		public void callback(CometEchoReponse echo) {
			// consume one result.
			if (echo != null) System.out.println("Continuable echo: " + echo.toString());
		}
	};

	void continuous() {
		RepeatBus client = new RepeatBus("bus-comet-client.xml");
		CometFacade comet = client.getService(CometFacade.class, callback);
		CometEchoReponse echo = comet.continuableEcho1("hello, world!");
		if (echo != null) System.err.println("Should be null: " + echo.toString());
		else System.out.println("Do be null.");
		while (true)
			CometContext.sleep(10000);
		// System.err.println("Should not be finished, except timeout/complete signal accept.");
	}

	void callback() {
		CallbackBus client = new CallbackBus("bus-comet-client.xml");
		CometFacade comet = client.getService(CometFacade.class, callback);
		for (int i = 0; i < 5; i++) {
			CometEchoReponse echo = comet.echo1("hello, world!", Math.round(Math.random() * 100),
					Math.round(Math.random() * 100), Math.round(Math.random() * 100));
			if (echo != null) System.out.println("ECHO: " + echo.toString());
			else System.err.println("Do be null.");
		}
		while (true)
			CometContext.sleep(10000);
	}
}
