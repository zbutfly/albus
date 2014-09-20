package net.butfly.bus.comet;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.Bus;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.context.Context;
import net.butfly.bus.ext.ContinuousBus;

public class CometClient {
	public static void main(String args[]) throws Exception {
		CometClient app = new CometClient();
		app.continuous();
	}

	void normal() {
		// final signal sig = new signal();
		Bus client = new Bus("bus-comet-client.xml");
		Context.sourceAppID("CometClientTest");
		CometFacade comet = client.getService(CometFacade.class);
		System.out.println("Sync echo: " + comet.echo("hello, world!"));
		System.out.println("Sync echo: " + comet.echo("hello, world!"));
		System.out.println("Sync echo: " + comet.echo("hello, world!"));
		System.out.println("Sync echo: " + comet.echo("hello, world!"));
	}

	void continuous() {
		ContinuousBus client = new ContinuousBus("bus-comet-client.xml");
		AsyncCallback<CometEchoReponse> callback = new AsyncCallback<CometEchoReponse>() {
			@Override
			public void callback(CometEchoReponse echo) {
				// consume one result.
				if (echo != null) System.out.println("Continuable echo: " + echo.toString());
			}
		};
		CometFacade comet = client.getService(CometFacade.class, callback);
		CometEchoReponse echo = comet.continuableEcho("hello, world!");
		if (echo != null) System.err.println("Should be null: " + echo.toString());
		else System.out.println("Do be null.");
		while (true)
			CometContext.sleep(10000);
		// System.err.println("Should not be finished, except timeout/complete signal accept.");
	}
}
