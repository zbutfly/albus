package net.butfly.bus.test;

import java.lang.reflect.Constructor;

import net.butfly.albacore.exception.BusinessException;
import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.bus.Bus;
import net.butfly.bus.context.Context;
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
		Context.initialize(null, remote);
		if (remote) {
			System.setProperty("bus.server.class", "net.butfly.bus.Bus");
			System.setProperty("bus.servlet.class", "net.butfly.bus.deploy.WebServiceServlet");
			System.setProperty("bus.server.base", "src/test/webapp");
			System.setProperty("bus.threadpool.size", "3");
			beforeBus(remote);
			logger.info("Remote test: bus server starting.");
			JettyStarter.main(getServerMainArguments());
			logger.info("Remote test: bus client starting.");
			client = new Bus(StringUtils.join(getClientConfiguration(), ','));
		} else {
			beforeBus(remote);
			logger.info("Local test: bus instance starting.");
			client = new Bus(StringUtils.join(getServerConfiguration(), ','));
		}
		beforeTest();
	}

	protected static void run() throws BusinessException {
		logger.info("==========================");
		logger.info("Local test: test starting.");
		logger.info("==========================");
		getInstance(false).doAllTest();
		logger.info("==========================");
		logger.info("Local test: test finished.");
		logger.info("==========================");
		logger.info("==========================");
		logger.info("Remote test: test starting.");
		logger.info("==========================");
		getInstance(true).doAllTest();
		logger.info("==========================");
		logger.info("Remote test: test finished.");
		logger.info("==========================");
		System.exit(0);
	};

	protected void doAllTest() throws BusinessException {}

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
	private static <T extends BusTest> T getInstance(Object remote) {
		try {
			Constructor<T> constructor = ((Class<T>) Class.forName(Thread.currentThread().getStackTrace()[3].getClassName()))
					.getDeclaredConstructor(boolean.class);
			return ReflectionUtils.safeConstruct(constructor, remote);
		} catch (Exception e) {
			return null;
		}
	}
}
