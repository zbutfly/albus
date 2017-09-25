package net.butfly.bus.start;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

class StarterParser {
	private CommandLineParser parser;
	private Options options;

	public StarterParser(Class<? extends CommandLineParser> parserClass) {
		try {
			this.parser = parserClass.getConstructor().newInstance();
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
			f.printUsage(pw, width, "java " + className + " [option(s)] [<config[@class]:]<path> ...");
			f.printWrapped(pw, width, "Start bus server(s) with CONFIG_FILE(s) (default bus.xml in root of classpath).");

			this.printWrapped(f, pw, "Example", "java -Dbus.jndi=context.xml " + className + " -k bus-server.xml");
			this.printWrapped(f, pw, "Example", "java -Dbus.jndi=context.xml " + className
					+ " -k bus1:bus-server1.xml bus2@xxx.yyy.MyBusServlet:bus-server2.xml");
			this.printWrapped(f, pw, "ContinuousOptions", null);
			f.printOptions(pw, width, options, f.getLeftPadding(), f.getDescPadding());
			this.printWrapped(f, pw, "Environment variables", null);
			this.printWrapped(f, pw, "bus.port", "Port of bus server (default " + Starter.DEFAULT_PORT + ")");
			this.printWrapped(f, pw, "bus.port.secure", "Secure port of bus server (default " + Starter.DEFAULT_SECURE_PORT + ")");
			this.printWrapped(f, pw, "bus.threadpool.size", "Thread pool size of bus server (default " + Starter.DEFAULT_THREAD_POOL_SIZE
					+ ", -1 for no threads pool)");
			this.printWrapped(f, pw, "bus.server.context", "Context path of bus server (default /" + Starter.DEFAULT_CONTEXT
					+ "/*), only used on one argument being defined.");
			this.printWrapped(f, pw, "bus.jndi", "Jndi context definition file (default no jndi resource attached)");
			this.printWrapped(f, pw, "bus.server.base", "Static resource root for bus server, such as index.html (default none)");
			this.printWrapped(f, pw, "bus.servlet.class",
					"Class name for the servlet of container of bus deployment (default net.butfly.bus.start.WebServiceServlet)");
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