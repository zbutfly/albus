package net.butfly.bus.start;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.butfly.albacore.utils.Objects;
import net.butfly.albacore.utils.Pair;
import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.impl.BusServlet;
import net.butfly.bus.impl.WebServiceServlet;

import org.apache.commons.cli.CommandLine;

final class StarterConfiguration {
	private String defaultContextPath;
	private Class<? extends BusServlet> defaultServletClass;

	boolean secure;
	int port;
	int sslPort;
	String resBase;
	int threads;
	boolean fork;
	String jndi;
	Map<String, Pair<List<String>, Class<? extends BusServlet>>> definitions;

	public StarterConfiguration(CommandLine cmd) {
		this.secure = cmd.hasOption('s');
		this.fork = cmd.hasOption('k');
		this.loadSystemProperties();
		this.parseBuses(cmd.getArgs());
	}

	public void parseBuses(String... args) {
		if (definitions == null)
			definitions = new HashMap<String, Pair<List<String>, Class<? extends BusServlet>>>(args.length);
		String[] defs;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			defs = arg.split(":");
			if (defs.length == 1) this.addDefinition(this.defaultContextPath, this.defaultServletClass, defs);
			else {
				String[] newdefs = defs[0].split("@", 2);
				if (newdefs.length == 1) this.addDefinition(defs[0], this.defaultServletClass,
						Arrays.copyOfRange(defs, 1, defs.length));
				else {
					Class<? extends BusServlet> servletClass = Reflections.forClassName(newdefs[1]);
					this.addDefinition(newdefs[0], servletClass, Arrays.copyOfRange(defs, 1, defs.length));
				}
			}
		}
	}

	private void addDefinition(String contextPath, Class<? extends BusServlet> servletClass, String[] configLocations) {
		Objects.noneNull(servletClass);
		Objects.notEmpty(contextPath);
		Objects.notEmpty(configLocations);
		if (!definitions.containsKey(contextPath)) definitions.put(contextPath,
				new Pair<List<String>, Class<? extends BusServlet>>(Arrays.asList(configLocations), servletClass));
		else {
			Pair<List<String>, Class<? extends BusServlet>> p = definitions.get(contextPath);
			if (!p.value2().isAssignableFrom(servletClass))
				throw new RuntimeException("Same context [" + contextPath + "], incompatible servlet class : ["
						+ p.value2().getName() + "] and [" + servletClass.getName() + "]");
			p.value2(servletClass);
			p.value1().addAll(Arrays.asList(configLocations));
		}
	}

	public StarterConfiguration(boolean fork, String... config) {
		this.fork = fork;
		this.loadSystemProperties();
	}

	private void loadSystemProperties() {
		this.port = Integer.getInteger("bus.port", Starter.DEFAULT_PORT);
		this.sslPort = Integer.getInteger("bus.port.secure", Starter.DEFAULT_SECURE_PORT);
		this.threads = Integer.getInteger("bus.threadpool.size", Starter.DEFAULT_THREAD_POOL_SIZE);
		this.jndi = System.getProperty("bus.jndi");
		this.resBase = System.getProperty("bus.server.base");

		this.defaultContextPath = System.getProperty("bus.server.context", Starter.DEFAULT_CONTEXT);
		this.defaultServletClass = Reflections.forClassName(System.getProperty("bus.servlet.class"));
		if (null == this.defaultServletClass) this.defaultServletClass = scanServletClass();
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

	private static Class<? extends BusServlet> scanServletClass() {
		Set<Class<? extends BusServlet>> classes = Reflections.getSubClasses(BusServlet.class);
		for (Class<? extends BusServlet> c : classes)
			if (!c.getName().startsWith("net.butfly.bus.")) return c;
		return WebServiceServlet.class;
	}
}