package net.butfly.bus.comet;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task.Callback;
import net.butfly.bus.CallbackBus;
import net.butfly.bus.comet.facade.CometFacade;

public class ContinuousTest extends CallbackTest {
	protected ContinuousTest(Mode mode) throws Exception {
		super(mode);
	}

	public static void main(String args[]) throws Exception {
		run();
		while (true)
			Thread.sleep(10000);
	}

	@Override
	protected void doAllTest() {
		this.echoString(3);
	}

	/*****************************************************/

	private void echoString(int retries) {
		String echo;
		try {
			echo = ((CallbackBus) this.client).service(CometFacade.class, echoCallback, new Options().retries(3))
					.echoString("hello, World!");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (echo != null) System.err.println("Should be null: " + echo.toString());
		else System.out.println("Do be null.");
	}

	protected final Callback<String> strCallback = new Callback<String>() {
		@Override
		public void callback(String echo) {
			// consume one result.
			if (echo != null) System.out.println("Continuous echo: " + echo.toString());
		}
	};
}
