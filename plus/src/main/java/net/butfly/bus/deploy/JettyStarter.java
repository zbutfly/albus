package net.butfly.bus.deploy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.albacore.utils.security.KeyStore;
import net.butfly.bus.Bus;
import net.butfly.bus.Constants;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
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
	protected final static int DEFAULT_PORT = 9876;
	protected final static int DEFAULT_THREAD_POOL_SIZE = 10;
	protected final Server server;
	protected boolean running = false;

	public JettyStarter() {
		this(DEFAULT_PORT);
	}

	public JettyStarter(int port) {
		this(port, DEFAULT_THREAD_POOL_SIZE);
	}

	public JettyStarter(int port, int threadPoolSize) {
		this.server = createServer(port, threadPoolSize, false);
	}

	public void run(boolean fork) {
		if (fork) {
			running = true;
			Thread th = new Thread(this);
			th.setDaemon(true);
			th.setName("Bus-Server-Jetty-Starter-Thread");
			th.start();
			while (running)
				try {
					Thread.sleep(30000);
				} catch (InterruptedException ex) {}
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

	public JettyStarter addBusInstance(String contextPath, String config, String resBase,
			Class<? extends BusServlet> servletClass, Class<? extends Bus> serverClass) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		if (null != resBase) context.setResourceBase(resBase);
		this.server.setHandler(context);

		context.addServlet(DefaultServlet.class, "/");

		ServletHolder servlet = new ServletHolder(servletClass);
		servlet.setDisplayName("BusServlet[" + null == config ? "DEFAULT" : config + "]");
		servlet.setInitOrder(0);
		if (null != config) servlet.setInitParameter("config-file", config);
		if (null != serverClass) servlet.setInitParameter("server-class", serverClass.getName());
		Method method;
		try {
			method = servletClass.getMethod("initializeBusParameters", Object.class);
		} catch (Exception e) {
			method = null;
		}
		if (null != method) method.invoke(null, servlet);
		context.addServlet(servlet, contextPath);
		return this;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends BusServlet> scanServletClass() {
		Set<Class<? extends BusServlet>> classes = ReflectionUtils.getSubClasses(BusServlet.class, "");
		for (Class<? extends BusServlet> c : classes)
			if (!c.getName().startsWith("net.butfly.bus.")) return c;
		try {
			return (Class<? extends BusServlet>) Class.forName(Constants.Configuration.RECOMMEND_EXTERNAL_SERVLET_CLASSNAME);
		} catch (ClassNotFoundException e) {}
		try {
			return (Class<? extends BusServlet>) Class.forName(Constants.Configuration.RECOMMEND_INTERNAL_SERVLET_CLASSNAME);
		} catch (ClassNotFoundException ee) {}
		return null;
	}

	protected Server createServer(int port, int threads, boolean https) {
		Server jetty = threads > 0 ? new Server(new QueuedThreadPool(threads)) : new Server();

		if (https) jetty.addConnector(this.httpsConnector(jetty, port, 60000, new KeyStore("/Users/butfly/.keystore", "123456",
				"123456")));
		else jetty.addConnector(this.httpConnector(jetty, port, 60000));
		return jetty;
	}

	protected Connector httpConnector(Server server, int port, long idle) {
		HttpConfiguration conf = new HttpConfiguration();
		conf.setSecureScheme("http");
		conf.setSecurePort(port);
		conf.setOutputBufferSize(BUF_SIZE);

		ServerConnector http = new NetworkTrafficServerConnector(server);
		http.setPort(port);
		http.setIdleTimeout(idle);
		return http;
	}

	protected Connector httpsConnector(Server server, int port, long idle, KeyStore keyStore) {
		HttpConfiguration conf = new HttpConfiguration();
		conf.setSecureScheme("https");
		conf.setSecurePort(8443);
		conf.setOutputBufferSize(8 * 1024);
		conf.addCustomizer(new SecureRequestCustomizer());

		SslContextFactory factory = new SslContextFactory();
		factory.setKeyStorePath(keyStore.getPath());
		factory.setKeyStorePassword(keyStore.getPassword());
		factory.setKeyManagerPassword(keyStore.getManagerPassword());

		ServerConnector https = new NetworkTrafficServerConnector(server, new SslConnectionFactory(factory,
				HttpVersion.HTTP_1_1.asString()), factory);
		https.setPort(port);
		https.setIdleTimeout(idle);

		return https;
	}

	public static void main(String args[]) throws Exception {
		net.butfly.albacore.logger.LoggerFactory.initialize();
		StarterParser parser = new StarterParser(PosixParser.class);
		CommandLine cmd = parser.parse(args);
		if (null != cmd) {
			StarterConfiguration conf = new StarterConfiguration(cmd);

			JettyStarter j = new JettyStarter(conf.port, conf.thread);
			j.addBusInstance(conf.context, conf.config, conf.resBase, conf.servletClass, conf.serverClass);
			if (null != conf.jdbc) addJNDI(conf.jdbc);
			j.run(conf.fork);
		}
	}

	public boolean starting() {
		return this.server.isStarting();
	}

	@SuppressWarnings("unchecked")
	public static void addJNDI(String contextXML) throws InstantiationException, IllegalAccessException, ClassNotFoundException,
			NamingException, DocumentException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(contextXML);
		if (null == contextXML) return;
		for (Element resource : (List<Element>) new SAXReader().read(url).getRootElement().selectNodes("Resource")) {
			BeanMap map = new BeanMap(Class.forName(resource.attributeValue("type")).newInstance());
			Iterator<Attribute> it = resource.attributeIterator();
			while (it.hasNext()) {
				Attribute attr = it.next();
				if ("name".equals(attr.getName()) || "type".equals(attr.getName())) continue;
				if (String.class.equals(map.getType(attr.getName()))) map.put(attr.getName(), attr.getValue());
				else if (int.class.equals(map.getType(attr.getName())) || Integer.class.equals(map.getType(attr.getName()))) map
						.put(attr.getName(), Integer.parseInt(attr.getValue()));
				else if (boolean.class.equals(map.getType(attr.getName())) || Boolean.class.equals(map.getType(attr.getName())))
					map.put(attr.getName(), Boolean.parseBoolean(attr.getValue()));

			}
			new Resource("java:comp/env/" + resource.attributeValue("name"), map.getBean());
		}
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
			this.options = this.options();
		}

		private Options options() {
			Options options = new Options();
			options.addOption("p", "port", true, "Port of bus server (default 9876)");
			options.addOption("t", "thread", true,
					"Thread pool size of bus server (default 10, -1 for no thread pool in server)");
			options.addOption("c", "context", true, "Context path of bus server (default \"" + DEFAULT_CONTEXT_PATH + "\")");
			options.addOption("f", "config", true, "Config file of bus (default bus.xml in root of classpath)");
			options.addOption("d", "jndi", true, "Jndi context definition file (default no jndi resource attached)");
			options.addOption("k", "fork", true,
					"Whether the bus server run in a forked thread (not daemon, the server will be stopped on main thread stopping, default false)");
			options.addOption("h", "help", false, "Print help for command line sterter of bus server");
			return options;
		}

		public CommandLine parse(String[] args) throws ParseException {
			CommandLine cmd = this.parser.parse(this.options, args);
			if (cmd.hasOption('h')) {
				StringBuilder footer = new StringBuilder();
				footer.append("Environment variables: \n");
				footer.append("    bus.class=<Class name for the core bus instance>\n");
				footer.append("        (Default: net.butfly.bus.Bus)\n");
				footer.append("    servlet.class=<Class name for the servlet of container of bus deployment>\n");
				footer.append("        (Default: net.butfly.bus.deploy.BusJSONServlet)");
				new HelpFormatter().printHelp("java <" + Thread.currentThread().getStackTrace()[1].getClassName()
						+ "> [-option]", "", options, footer.toString());
				return null;
			} else return cmd;
		}
	}

	private static class StarterConfiguration {
		private String resBase;
		private int port;
		private int thread;
		private String context;
		private String config;
		private String jdbc;
		private boolean fork;
		private Class<? extends BusServlet> servletClass;
		private Class<? extends Bus> serverClass;

		@SuppressWarnings("unchecked")
		public StarterConfiguration(CommandLine cmd) throws ClassNotFoundException {
			this.port = cmd.hasOption('p') ? Integer.parseInt(cmd.getOptionValue('p')) : DEFAULT_PORT;
			this.thread = cmd.hasOption('t') ? Integer.parseInt(cmd.getOptionValue('t')) : DEFAULT_THREAD_POOL_SIZE;
			this.context = cmd.hasOption('c') ? cmd.getOptionValue('c') : DEFAULT_CONTEXT_PATH;
			this.config = cmd.hasOption('f') ? cmd.getOptionValue('f') : null;
			this.jdbc = cmd.hasOption('d') ? cmd.getOptionValue('d') : null;
			this.fork = cmd.hasOption('k') ? Boolean.parseBoolean(cmd.getOptionValue('k')) : false;
			String servletClassName = System.getProperty("servlet.class");// cmd.getOptionValue('e');
			String serverClassName = System.getProperty("bus.class");// cmd.getOptionValue('b');
			this.resBase = System.getProperty("server.base");
			this.servletClass = null == servletClassName ? scanServletClass() : (Class<? extends BusServlet>) Class
					.forName(servletClassName);
			this.serverClass = null == serverClassName ? Bus.class : (Class<? extends Bus>) Class.forName(serverClassName);
		}
	}
}