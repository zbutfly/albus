package net.butfly.bus.demo;

import net.butfly.bus.Bus;
import net.butfly.bus.impl.BusFactory;
import net.butfly.bus.start.JettyStarter;

public class Demo {
	public static void main(String[] args) throws Exception {
		demo("demo-beanfactory.xml");
		demo("demo-spring.xml");
	}

	private static void demo(String configFile) throws Exception {
		System.out.println("  = Demo for Albus: " + configFile);
		// Local
		System.out.println(" == Local invoking...");
		Bus bus = BusFactory.client(configFile);
		String r = bus.invoke("SPL_001", new Object[] { "Hello, world!" });
		System.out.print("Local invoking return: ");
		System.out.println(r);
		System.out.println("Local invoking finished.");

		// Webservice
		System.out.println(" == Demo for webservice invoking...");
		JettyStarter t = JettyStarter.fork("demo:" + configFile);

		Bus client = BusFactory.client("demo-client.xml");
		try {
			String cr = client.invoke("SPL_001", new Object[] { "Hello, world!" });
			System.out.print("Webservice invoking return: ");
			System.out.println(cr);
			System.out.println("Webservice invoking finished.");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			t.shutdown();
		}
		System.out.println("============");
		System.out.println("===========");
		System.out.println("==========");
		System.out.println("=========");
		System.out.println("========");
		System.out.println("=======");
		System.out.println("======");
		System.out.println("=====");
		System.out.println("====");
	}
}
