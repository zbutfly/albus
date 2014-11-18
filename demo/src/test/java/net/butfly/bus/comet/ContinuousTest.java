package net.butfly.bus.comet;

import net.butfly.albacore.utils.async.Callback;
import net.butfly.bus.comet.facade.CometFacade;
import net.butfly.bus.ext.Bus;
import net.butfly.bus.utils.async.Options;

public class ContinuousTest extends CallbackTest {
	protected ContinuousTest(boolean remote) throws Exception {
		super(remote);
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

	@Override
	protected void beforeTest() {}

	/*****************************************************/

	private void echoString(int retries) {
		String echo = ((Bus) this.client).getService(CometFacade.class, echoCallback, new Options().retries(3)).echoString(
				"hello, World!");
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
