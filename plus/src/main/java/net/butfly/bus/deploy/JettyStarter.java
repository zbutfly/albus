package net.butfly.bus.deploy;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.utils.JNDIUtils;
import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.albacore.utils.async.AsyncUtils;
import net.butfly.bus.impl.BusServlet;
import net.butfly.bus.impl.WebServiceServlet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.jetty.http.spi.DelegatingThreadPool;
import org.eclipse.jetty.plus.jndi.Resource;
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

public class JettyStarter implements Runnable {
	protected final static Logger logger = LoggerFactory.getLogger(JettyStarter.class);
	protected final int BUF_SIZE = 8 * 1024;
	protected final static String DEFAULT_CONTEXT_PATH = "/bus/*";
	protected final static String DEFAULT_PORT = "19080";
	protected final static String DEFAULT_SECURE_PORT = "19443";
	protected final static String DEFAULT_THREAD_POOL_SIZE = "-1";
	private static final long DEFAULT_IDLE = 60000;
	protected final Server server;
	protected final ServletContextHandler context;
	protected boolean running = false;

	public JettyStarter() {
		this(new StarterConfiguration(new String[] { null }, false));
	}

	public JettyStarter(StarterConfiguration conf) {
		if (conf.threads > 0) this.server = new Server(new QueuedThreadPool(conf.threads));
		else if (conf.threads == 0) this.server = new Server(new DelegatingThreadPool(AsyncUtils.getDefaultExecutor()));
		else this.server = new Server();
		this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		this.context.setContextPath("/");
		if (null != conf.resBase) context.setResourceBase(conf.resBase);
		this.server.setHandler(context);
		this.context.addServlet(DefaultServlet.class, "/");
		if (conf.secure) createSSLServer(conf.port, conf.sslPort);
		else createServer(conf.port);

	}

	public void run(boolean fork) {
		if (fork) {
			Thread th = new Thread(this);
			th.setDaemon(true);
			th.setName("BusImpl-Server-Jetty-Starter-Thread");
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

	public JettyStarter addBusInstance(StarterConfiguration conf) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		for (String cfg : conf.config) {
			ServletHolder servlet = new ServletHolder(conf.servletClass);
			servlet.setDisplayName("BusServlet[" + null == cfg ? "DEFAULT" : cfg + "]");
			servlet.setInitOrder(0);
			if (null != cfg) servlet.setInitParameter("config-file", cfg);
			for (Field f : conf.servletClass.getDeclaredFields()) {
				Object a = f.getAnnotation(ServletInitParams.class);
				if (null != a && Map.class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) {
					Map<String, String> params = ReflectionUtils.safeFieldGet(f, null);
					for (String name : params.keySet())
						servlet.setInitParameter(name, params.get(name));
				}
			}
			context.addServlet(servlet, conf.context);
		}
		return this;
	}

	private static Class<? extends BusServlet> scanServletClass() {
		Set<Class<? extends BusServlet>> classes = ReflectionUtils.getSubClasses(BusServlet.class, "");
		for (Class<? extends BusServlet> c : classes)
			if (!c.getName().startsWith("net.butfly.bus.")) return c;
		return WebServiceServlet.class;
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
			j.addBusInstance(conf);
			if (null != conf.jndi) addJNDI(conf.jndi);
			j.run(conf.fork);
		}
	}

	public boolean starting() {
		return this.server.isStarting();
	}

	private static class StarterParser {
		private CommandLineParser parser;
		private Options options;

		public StarterParser(Class<? extends CommandLineParser> parserClass) {
			try {
				this.parser = parserClass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			options = new Options();
			this.options();
		}

		private void options() {
			options.addOption("s", "secure", false, "If presented, bus server will open https service (secure service).");
			options.addOption("k", "fork", false,
					"If presented, bus server will run in a forked threads (not daemon, the server will be stopped on main threads stopping)");

			options.addOption("h", "help", false, "Print help for command line sterter of bus server");
		}

		public CommandLine parse(String[] args) throws ParseException {
			CommandLine cmd = this.parser.parse(this.options, args, true);
			if (cmd.hasOption('h')) {
				PrintWriter pw = new PrintWriter(System.out);
				HelpFormatter f = new HelpFormatter();
				f.setWidth(94);
				int width = f.getWidth();
				String className = Thread.currentThread().getStackTrace()[2].getClassName();
				f.printUsage(pw, width, "java " + className + " [OPTION] [CONFIG_FILE ...]");
				f.printWrapped(pw, width, "Start bus server(s) with CONFIG_FILE(s) (default bus.xml in root of classpath).");

				this.printWrapped(f, pw, "Example", "java -Dbus.jndi=context.xml " + className + " -k bus-server.xml");
				this.printWrapped(f, pw, "ContinuousOptions", null);
				f.printOptions(pw, width, options, f.getLeftPadding(), f.getDescPadding());
				this.printWrapped(f, pw, "Environment variables", null);
				this.printWrapped(f, pw, "bus.port", "Port of bus server (default " + DEFAULT_PORT + ")");
				this.printWrapped(f, pw, "bus.port.secure", "Secure port of bus server (default " + DEFAULT_SECURE_PORT + ")");
				this.printWrapped(f, pw, "bus.threadpool.size", "Thread pool size of bus server (default "
						+ DEFAULT_THREAD_POOL_SIZE + ", -1 for no threads pool)");
				this.printWrapped(f, pw, "bus.server.context", "Context path of bus server (default " + DEFAULT_CONTEXT_PATH
						+ ")");
				this.printWrapped(f, pw, "bus.jndi", "Jndi context definition file (default no jndi resource attached)");
				this.printWrapped(f, pw, "bus.server.base",
						"Static resource root for bus server, such as index.html (default none)");
				this.printWrapped(f, pw, "bus.servlet.class",
						"Class name for the servlet of container of bus deployment (default net.butfly.bus.deploy.WebServiceServlet)");
				pw.flush();
				return null;
			} else return cmd;
		}

		private void printWrapped(final HelpFormatter f, final PrintWriter pw, String prefix, String desc) {
			prefix = prefix + ": ";
			if (desc != null) prefix = prefix + "\n\t" + desc;
			f.printWrapped(pw, f.getWidth(), 8, prefix);
		}
	}

	private static final class StarterConfiguration {
		private boolean secure;
		private int port;
		private int sslPort;
		private String resBase;
		private int threads;
		private boolean fork;
		private String jndi;
		private String context;
		private String[] config;
		private Class<? extends BusServlet> servletClass;

		public StarterConfiguration(CommandLine cmd) {
			this.config = cmd.getArgs();
			this.secure = cmd.hasOption('s');
			this.fork = cmd.hasOption('k');
			this.loadSystemProperties();
		}

		public StarterConfiguration(String[] config, boolean fork) {
			this.config = config;
			this.fork = fork;
			this.loadSystemProperties();
		}

		@SuppressWarnings("unchecked")
		private void loadSystemProperties() {
			this.port = Integer.parseInt(System.getProperty("bus.port", DEFAULT_PORT));
			this.sslPort = Integer.parseInt(System.getProperty("bus.port.secure", DEFAULT_SECURE_PORT));
			this.threads = Integer.parseInt(System.getProperty("bus.threadpool.size", DEFAULT_THREAD_POOL_SIZE));
			this.context = System.getProperty("bus.server.context", DEFAULT_CONTEXT_PATH);
			this.jndi = System.getProperty("bus.jndi");
			this.resBase = System.getProperty("bus.server.base");

			try {
				// cmd.getOptionValue('e');
				this.servletClass = (Class<? extends BusServlet>) Class.forName(System.getProperty("bus.servlet.class"));
			} catch (Throwable t) {
				this.servletClass = scanServletClass();
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Start configuration:\n");
			for (Field f : this.getClass().getDeclaredFields())
				if (f.getType().isArray()) {
					sb.append("\t").append(f.getName()).append(": \n");
					try {
						for (Object e : (Object[]) f.get(this))
							sb.append("\t\t").append(e).append("\n");
					} catch (IllegalAccessException e) {}
				} else try {
					sb.append("\t").append(f.getName()).append(": ").append((Object) f.get(this)).append("\n");
				} catch (IllegalAccessException e) {}
			return sb.toString();
		}
	}

	public static void addJNDI(String contextXml) {
		JNDIUtils.addJNDI(contextXml, Resource.class.getName());
	}
}