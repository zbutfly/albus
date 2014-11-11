package net.butfly.bus.comet;

import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.bus.RepeatBus;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;

public class CometLocal {
	public static void main(String[] args) {
		RepeatBus client = new RepeatBus("bus-comet-local.xml");
		AsyncCallback<CometEchoReponse> callback = new AsyncCallback<CometEchoReponse>() {
			@Override
			public void callback(CometEchoReponse echo) {
				if (echo != null) System.out.println("Continuable echo: " + echo.toString());
			}
		};
		CometFacade comet = client.getService(CometFacade.class, callback);
		System.out.println("Comet facade return:" + comet.echoString("hello, world!"));
		System.out.println("Comet facade return:" + comet.echoCompose("hello, world!", 1, 2, 3));
		comet = client.getService(CometFacade.class, callback, 0, 0);
		comet.echoString("Hello, Comet!");
		System.out.println("finished.");
	}
}
