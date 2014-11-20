package net.butfly.bus.comet;

import net.butfly.albacore.utils.async.Callback;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;
import net.butfly.bus.ext.Bus;

public class CallbackTest extends StandardTest {
	protected CallbackTest(boolean remote) throws Exception {
		super(remote);
	}

	public static void main(String args[]) throws Exception {
		run();
		while (true)
			Thread.sleep(10000);
	}

	@Override
	protected void doAllTest() {
		for (int i = 0; i < 5; i++)
			this.composetest(i);
	}

	@Override
	protected void beforeTest() {
//		this.enableLocal(false);
		this.echoCallback = new Callback<CometEchoReponse>() {
			@Override
			public void callback(CometEchoReponse echo) {
				// consume one result.
				if (echo != null) System.out.println("Callback echo: " + echo.toString());
			}
		};
		this.facade = ((Bus) this.client).getService(CometFacade.class, echoCallback);
	}

	/*****************************************************/

	protected Callback<CometEchoReponse> echoCallback;
}
