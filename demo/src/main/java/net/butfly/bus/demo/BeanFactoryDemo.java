package net.butfly.bus.demo;

import java.util.concurrent.Future;

import net.butfly.albacore.utils.async.Concurrents;
import net.butfly.bus.Bus;
import net.butfly.bus.impl.BusFactory;
import net.butfly.bus.start.JettyStarter;

public class BeanFactoryDemo {
	public static void main(String[] args) throws Exception {
		System.out.println("  = Demo for Albus: BeanFactory     = ");
		System.out.println(" == Demo for local invoking...      == ");
		Bus bus = BusFactory.client("demo-beanfactory.xml");
		String r = bus.invoke("SPL_001", new Object[] { "Hello, world!" });
		System.out.print("Local invoking return: ");
		System.out.println(r);
		System.out.println("Local invoking finished.");

		// Webservice server
		System.out.println(" == Demo for webservice invoking... == ");
		Thread t = JettyStarter.thread("demo:demo-beanfactory.xml");
		Future<?> f = Concurrents.submit(t);

		Bus client = BusFactory.client("demo-client.xml");
		try {
			String cr = client.invoke("SPL_001", new Object[] { "Hello, world!" });
			System.out.print("Webservice invoking return: ");
			System.out.println(cr);
			System.out.println("Webservice invoking finished.");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			f.cancel(true);
		}
	}
}
