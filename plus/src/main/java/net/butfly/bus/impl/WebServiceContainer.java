//package net.butfly.bus.impl;
//
//import java.util.Map;
//
//import javax.servlet.Servlet;
//
//import net.butfly.bus.Bus.Mode;
//import net.butfly.bus.policy.Router;
//import net.butfly.bus.serialize.Serializer;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class WebServiceContainer extends ContainerBase<Servlet> implements Container<Servlet> {
//	private static Logger logger = LoggerFactory.getLogger(WebServiceContainer.class);
//	private Cluster cluster;
//	private Router router;
//	private Map<String, Serializer> serializerMap;
//
//	public WebServiceContainer(String[] configs, String serializerClasses, String clusterRouterClass) {
//		cluster = new Cluster(Mode.SERVER, serializerClasses, clusterRouterClass, false, configs);
//		logger.info("Bus servlet inited.");
//
//	}
//
//}
