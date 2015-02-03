package net.butfly.bus.comet;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.utils.async.Task.Callback;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.comet.facade.dto.CometEchoReponse;

public class CallbackTest extends StandardTest {
	protected CallbackTest(Mode mode) throws Exception {
		super(mode);
		this.echoCallback = new Callback<CometEchoReponse>() {
			@Override
			public void callback(CometEchoReponse echo) {
				// consume one result.
				if (echo != null) System.out.println("Callback echo: " + echo.toString());
			}
		};
		this.facade = ((CallbackBus) this.client).service(CometFacade.class, echoCallback);
	}

	public static void main(String args[]) throws Exception {
		run();
		while (true)
			Thread.sleep(10000);
	}

	@Override
	protected void doAllTest() throws BusinessException {
		for (int i = 0; i < 5; i++)
			this.composetest(i);
	}

	/*****************************************************/

	protected Callback<CometEchoReponse> echoCallback;
}
