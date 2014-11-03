package net.butfly.bus.server;

import net.butfly.bus.deploy.JettyStarter;

public class CometServer {
	public static void main(String[] args) throws Exception {
		JettyStarter.main("-h");
		System.setProperty("bus.server.class", "net.butfly.bus.RepeatBus");
		System.setProperty("bus.servlet.class", "net.butfly.bus.deploy.BusWebServiceServlet");
		System.setProperty("bus.server.base", "src/test/webapp");
		JettyStarter.main("bus-comet-server.xml");
	}
}
