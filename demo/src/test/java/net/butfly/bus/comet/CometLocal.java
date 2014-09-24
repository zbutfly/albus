package net.butfly.bus.comet;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.ext.ContinuousBus;

public class CometLocal {
	public static void main(String[] args) {
		ContinuousBus client = new ContinuousBus("bus-comet-local.xml");
		AsyncCallback<CometEchoReponse> callback = new AsyncCallback<CometEchoReponse>() {
			@Override
			public void callback(CometEchoReponse echo) {
				if (echo != null) System.out.println("Continuable echo: " + echo.toString());
			}
		};
		CometFacade comet = client.getService(CometFacade.class, callback);
		comet.continuableEcho("Hello, Comet!");
		System.out.println("Comet facade return:" + comet.echo("hello, world!"));
		System.out.println("Comet facade return:" + comet.echo("hello, world!"));
		System.out.println("Comet facade return:" + comet.echo("hello, world!"));
		System.out.println("finished.");
	}
}
