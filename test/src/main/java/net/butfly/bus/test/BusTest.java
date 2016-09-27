package net.butfly.bus.test;

import net.butfly.albacore.utils.logger.Logger;

import com.google.common.base.Joiner;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.Bus;
import net.butfly.bus.impl.BusFactory;
import net.butfly.bus.start.JettyStarter;

public abstract class BusTest {
	protected enum Mode {
		LOCAL, REMOTE, CLIENT, SERVER
	}

	protected static final Logger logger = Logger.getLogger(BusTest.class);

	protected Mode mode;
	protected Bus client;

	protected BusTest(Mode mode) throws Exception {
		this.mode = mode;
		if (mode == Mode.REMOTE || mode == Mode.SERVER) {
			System.setProperty("bus.server.waiting", "true");
			logger.info(mode.name() + " test: bus server starting.");
			JettyStarter.main(getServerMainArguments());
		}
		if (mode != Mode.SERVER) {
			logger.info(mode.name() + " test: bus client starting.");
			client = BusFactory.client(this.getClientConfigurationForType(this.isRemote()));
		}
	}

	protected final String getClientConfigurationForType(boolean remote) {
		return Joiner.on(',').join(remote ? this.getClientConfiguration() : this.getServerConfiguration());
	}

	protected static void run(Mode... mode) throws Exception {
		if (null == mode || mode.length == 0) mode = new Mode[] { Mode.LOCAL, Mode.REMOTE };

		for (Mode m : mode)
			getTestInstance(m).doTestWrapper();
	};

	protected abstract void doAllTest() throws BusinessException;

	protected String[] getClientConfiguration() {
		return null;
	}

	protected String[] getServerConfiguration() {
		return null;
	}

	protected String[] getServerMainArguments() {
		return new String[] { "-k", Joiner.on(',').join(getServerConfiguration()) };
	}

	protected boolean isRemote() {
		return mode != Mode.LOCAL && mode != Mode.SERVER;
	}

	@SuppressWarnings("unchecked")
	private static <T extends BusTest> T getTestInstance(Mode mode) throws BusinessException {
		return (T) Reflections.construct(Reflections.getMainClass(), mode);
	}

	private void doTestWrapper() throws BusinessException {
		if (this.isRemote()) while (Boolean.parseBoolean(System.getProperty("bus.server.waiting")))
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {}
		if (this.mode == Mode.SERVER) {
			logger.info("==========================");
			logger.info(mode.name() + "@[" + this.getClass().getName() + "] test: server ready, waiting for client...");
			logger.info("==========================");
			while (true)
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {}
		} else {
			logger.info("==========================");
			logger.info(mode.name() + "@[" + this.getClass().getName() + "] test: test starting.");
			logger.info("==========================");
			doAllTest();
			logger.info("==========================");
			logger.info(mode.name() + "@[" + this.getClass().getName() + "] test: test finished.");
			logger.info("==========================");
		}
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
