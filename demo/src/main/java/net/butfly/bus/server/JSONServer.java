package net.butfly.bus.server;

import net.butfly.bus.deploy.JettyStarter;

public class JSONServer {
	public static void main(String[] args) throws Exception {
		JettyStarter.main(new String[] { "-h" });
		System.setProperty("bus.class", "net.butfly.bus.BasicBus");
		System.setProperty("servlet.class", "net.butfly.bus.deploy.BusJSONServlet");
		System.setProperty("server.base", "src/test/webapp");
		JettyStarter.main(new String[] { "-f", "bus-comet-server.xml" });
	}
}
