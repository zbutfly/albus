package net.butfly.bus.server;

import net.butfly.bus.deploy.JettyStarter;

public class CometServer {
	public static void main(String[] args) throws Exception {
		JettyStarter.main(new String[] { "-h" });
		System.setProperty("bus.class", "net.butfly.bus.ext.ContinuousBus");
		System.setProperty("servlet.class", "net.butfly.bus.deploy.BusHessianStreamingServlet");
		JettyStarter.main(new String[] { "-f", "bus-comet-server.xml" });
	}
}
