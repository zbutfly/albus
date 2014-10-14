package net.butfly.bus.server;

import net.butfly.bus.deploy.JettyStarter;

public class CometServer {
	public static void main(String[] args) throws Exception {
		JettyStarter.main("-h");
		System.setProperty("bus.server.class", "net.butfly.bus.ext.ContinuousBus");
		System.setProperty("bus.servlet.class", "net.butfly.bus.deploy.BusHessianServlet");
		System.setProperty("bus.keystore.password", "password");
		System.setProperty("bus.keymanager.password", "password");
//		System.setProperty("bus.trust.keystore.password", "");
		JettyStarter.main("bus-comet-server.xml");
	}
}
