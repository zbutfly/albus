package net.butfly.bus.test;

import java.lang.reflect.Constructor;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.bus.Bus;
import net.butfly.bus.deploy.JettyStarter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BusTest {
	protected static final Logger logger = LoggerFactory.getLogger(BusTest.class);

	private boolean remote;
	protected Bus client;

	protected BusTest(boolean remote) throws Exception {
		this.remote = remote;
		if (remote) {
			System.setProperty("bus.server.class", getBusClass().getName());
			System.setProperty("bus.servlet.class", "net.butfly.bus.deploy.WebServiceServlet");
			System.setProperty("bus.server.base", "src/test/webapp");
			System.setProperty("bus.threadpool.size", "3");
			beforeBus(remote);
			logger.info("Remote test: bus server starting.");
			JettyStarter.main(getServerMainArguments());
			logger.info("Remote test: bus client starting.");
			client = getBusInstance(getBusClass(), StringUtils.join(getClientConfiguration(), ','));
		} else {
			beforeBus(remote);
			logger.info("Local test: bus instance starting.");
			client = getBusInstance(getBusClass(), StringUtils.join(getServerConfiguration(), ','));
		}
		beforeTest();
	}

	protected static void run(boolean... isRemote) throws Exception {
		if (null == isRemote) isRemote = new boolean[] { false, true };

		for (boolean remote : isRemote)
			getTestInstance(remote).doTestWrapper();
	};

	protected void doAllTest() throws BusinessException {}

	protected Class<? extends Bus> getBusClass() {
		return Bus.class;
	}

	protected String[] getClientConfiguration() {
		return null;
	}

	protected String[] getServerConfiguration() {
		return null;
	}

	protected String[] getServerMainArguments() {
		return new String[] { "-k", StringUtils.join(getServerConfiguration(), ',') };
	}

	protected void beforeBus(boolean remote) throws Exception {}

	protected void beforeTest() {}

	protected boolean isRemote() {
		return this.remote;
	}

	@SuppressWarnings("unchecked")
	private static <T extends BusTest> T getTestInstance(Object remote) throws Exception {
		Constructor<T> constructor = ((Class<T>) Class.forName(Thread.currentThread().getStackTrace()[3].getClassName()))
				.getDeclaredConstructor(boolean.class);
		return ReflectionUtils.safeConstruct(constructor, remote);
	}

	private static Bus getBusInstance(Class<? extends Bus> clazz, String conf) throws Exception {
		return clazz.getConstructor(String.class).newInstance(conf);
	}

	private void doTestWrapper() throws BusinessException {
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
