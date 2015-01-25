package net.butfly.bus.test;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.Texts;
import net.butfly.bus.Bus;
import net.butfly.bus.impl.BusFactory;
import net.butfly.bus.impl.WebServiceServlet;
import net.butfly.bus.start.JettyStarter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BusTest {
	protected static final Logger logger = LoggerFactory.getLogger(BusTest.class);

	private boolean remote;
	protected Bus client;

	protected BusTest(boolean remote) throws Exception {
		this.remote = remote;
		if (remote) {
			System.setProperty("bus.servlet.class", WebServiceServlet.class.getName());
			System.setProperty("bus.server.base", "src/test/webapp");
			System.setProperty("bus.threadpool.size", "3");
			// System.setProperty("bus.invoker.spring.lazy", "true");
			System.setProperty("bus.server.waiting", "true");
			logger.info("Remote test: bus server starting.");
			JettyStarter.main(getServerMainArguments());
			logger.info("Remote test: bus client starting.");
		} else {
			logger.info("Local test: bus instance starting.");
		}
		client = BusFactory.client(this.getClientConfigurationForType(remote));
	}

	protected final String getClientConfigurationForType(boolean remote) {
		return Texts.join(',', remote ? this.getClientConfiguration() : this.getServerConfiguration());
	}

	protected static void run(boolean... isRemote) throws Exception {
		if (null == isRemote || isRemote.length == 0) isRemote = new boolean[] { false, true };

		for (boolean remote : isRemote)
			getTestInstance(remote).doTestWrapper();
	};

	protected abstract void doAllTest() throws BusinessException;

	protected String[] getClientConfiguration() {
		return null;
	}

	protected String[] getServerConfiguration() {
		return null;
	}

	protected String[] getServerMainArguments() {
		return new String[] { "-k", Texts.join(',', getServerConfiguration()) };
	}

	protected boolean isRemote() {
		return this.remote;
	}

	@SuppressWarnings("unchecked")
	private static <T extends BusTest> T getTestInstance(Object remote) throws BusinessException {
		return (T) Reflections.construct(Reflections.getMainClass(),
				Reflections.parameters(boolean.class, remote));
	}

	private void doTestWrapper() throws BusinessException {
		if (this.isRemote()) while (Boolean.parseBoolean(System.getProperty("bus.server.waiting")))
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {}
		String desc = (remote ? "Remote" : "Local");
		logger.info("==========================");
		logger.info(desc + " test: test starting.");
		logger.info("==========================");
		doAllTest();
		logger.info("==========================");
		logger.info(desc + " test: test finished.");
		logger.info("==========================");
	}

	protected static final void waiting() {
		while (true)
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
	}

	protected static final void finish() {
		System.exit(0);
	}
}
