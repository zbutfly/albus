package net.butfly.bus.start;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.eclipse.jetty.http.spi.DelegatingThreadPool;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import net.butfly.albacore.utils.Objects;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.async.Task;
import net.butfly.albacore.utils.more.JNDIUtils;
import net.butfly.bus.impl.BusServlet;
import net.butfly.bus.impl.ServletInitParams;

public class JettyStarter implements Runnable {
	protected static final Logger logger = LoggerFactory.getLogger(JettyStarter.class);
	protected static final int BUF_SIZE = 8 * 1024;
	protected static final long DEFAULT_IDLE = 60000;
	protected final Server server;
	protected final ServletContextHandler handler;
	protected boolean running = false;

	public JettyStarter(StarterConfiguration conf) {
		if (conf.threads > 0) this.server = new Server(new QueuedThreadPool(conf.threads));
		else if (conf.threads == 0) this.server = new Server(new DelegatingThreadPool(Task.getDefaultExecutor()));
		else this.server = new Server();
		this.handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		this.handler.setContextPath("/");
		if (null != conf.resBase) handler.setResourceBase(conf.resBase);
		this.server.setHandler(handler);
		this.handler.addServlet(DefaultServlet.class, "/");
		if (conf.secure) createSSLServer(conf.port, conf.sslPort);
		else createServer(conf.port);

	}

	public void run(boolean fork) {
		if (fork) {
			Thread th = new Thread(this);
			th.setDaemon(true);
			th.setName("StandardBus-Server-Jetty-Starter-Thread");
			th.start();
		} else this.run();
	}

	@Override
	public void run() {
		try {
			logger.trace("Jetty starting...");
			server.start();
			server.join();
			logger.trace("Jetty started.");
		} catch (Exception e) {
			logger.error("Jetty starting failure: ", e);
			running = false;
			throw new RuntimeException(e);
		}
	}

	@Override
	public void finalize() throws Exception {
		if (server.isRunning()) {
			logger.trace("Jetty stopping...");
			server.stop();
			logger.trace("Jetty stopped.");
		}
	}

	public JettyStarter addBusInstances(StarterConfiguration conf) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		for (String contextPath : conf.definitions.keySet())
			this.addBusInstance(contextPath, conf.definitions.get(contextPath).value2(), conf.definitions.get(contextPath).value1().toArray(
					new String[0]));
		new RuntimeException("Command line argument should has format [busConfigFile[@busServletClass]:]<servletContextPath> ...");
		return this;
	}

	private void addBusInstance(String contextPath, Class<? extends BusServlet> servletClass, String... configLocation) {
		Objects.notNull(servletClass);
		Objects.notEmpty(contextPath);
		Objects.notEmpty(configLocation);
		ServletHolder servlet = new ServletHolder(servletClass);
		servlet.setAsyncSupported(true);
		servlet.setDisplayName("BusServlet[" + contextPath + "@" + servletClass.getName() + "]");
		servlet.setInitOrder(0);
		servlet.setInitParameter("config", Joiner.on(',').join(configLocation));
		for (Field f : servletClass.getDeclaredFields()) {
			Annotation a = f.getAnnotation(ServletInitParams.class);
			if (null != a && Map.class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) {
				Map<String, String> params = Reflections.get(null, f);
				for (String name : params.keySet())
					servlet.setInitParameter(name, params.get(name));
			}
		}
		if (!contextPath.startsWith("/")) contextPath = "/" + contextPath;
		if (!contextPath.endsWith("/*")) contextPath = contextPath + "/*";
		this.handler.addServlet(servlet, contextPath);
		logger.info("Servlet " + servletClass.getName() + " is registeried to \"" + contextPath + "\" with configuration(s): " + Joiner.on(
				',').join(configLocation) + "");
	}

	protected void createServer(int port) {
		HttpConfiguration conf = new HttpConfiguration();
		conf.setSecureScheme("http");
		conf.setSecurePort(port);
		conf.setOutputBufferSize(BUF_SIZE);

		ServerConnector http = new NetworkTrafficServerConnector(server);
		http.setPort(port);
		http.setIdleTimeout(DEFAULT_IDLE);
		this.server.addConnector(http);
	}

	protected void createSSLServer(int port, int sslPort) {
		// Setup SSL
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(System.getProperty("bus.keystore.path", "keystore.jks"));
		sslContextFactory.setKeyStorePassword(System.getProperty("bus.keystore.password"));
		sslContextFactory.setKeyManagerPassword(System.getProperty("bus.keymanager.password"));

		// Two-way SSL
		// sslContextFactory.setNeedClientAuth(true);
		// sslContextFactory.setTrustStorePath(System.getProperty("bus.trust.keystore.path",
		// "truststore.jks"));
		// sslContextFactory.setTrustStorePassword(System.getProperty("bus.trust.keystore.password"));

		// Setup HTTP Configuration
		HttpConfiguration httpConf = new HttpConfiguration();
		httpConf.setSecurePort(sslPort);
		httpConf.setSecureScheme("https");

		ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConf));
		httpConnector.setName("unsecured"); // named connector
		httpConnector.setPort(port);

		// Setup HTTPS Configuration
		HttpConfiguration httpsConf = new HttpConfiguration(httpConf);
		httpsConf.addCustomizer(new SecureRequestCustomizer());

		ServerConnector httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"),
				new HttpConnectionFactory(httpsConf));
		httpsConnector.setName("secured"); // named connector
		httpsConnector.setPort(sslPort);

		// Add connectors
		server.setConnectors(new Connector[] { httpConnector, httpsConnector });

		// Wire up contexts for secure handling to named connector
		// String secureHosts[] = new String[] { "@secured" };

		// Wire up context for unsecure handling to only
		// the named 'unsecured' connector
		ContextHandler redirectHandler = new ContextHandler();
		redirectHandler.setContextPath("/");
		redirectHandler.setHandler(new SecuredRedirectHandler());
		redirectHandler.setVirtualHosts(new String[] { "@unsecured" });

		// Establish all handlers that have a context
		ContextHandlerCollection contextHandlers = new ContextHandlerCollection();
		contextHandlers.setHandlers(new Handler[] { redirectHandler });

		// Create server level handler tree
		HandlerList handlers = new HandlerList();
		handlers.addHandler(contextHandlers);
		handlers.addHandler(new DefaultHandler()); // round things out

		server.setHandler(handlers);
	}

	public static void main(String... args) throws Exception {
		StarterParser parser = new StarterParser(PosixParser.class);
		CommandLine cmd = parser.parse(args);
		if (null != cmd) {
			StarterConfiguration conf = new StarterConfiguration(cmd);
			logger.info(conf.toString());

			JettyStarter j = new JettyStarter(conf);
			j.addBusInstances(conf);
			if (null != conf.jndi) JNDIUtils.attachContext(conf.jndi);
			j.run(conf.fork);
		}
	}

	public boolean starting() {
		return this.server.isStarting();
	}
}