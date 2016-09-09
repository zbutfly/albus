package net.butfly.bus.deploy;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import net.butfly.bus.deploy.entry.EntryPoint;
import net.butfly.bus.deploy.entry.EntryPointImpl;
import net.butfly.bus.hessian.SimpleHessianServlet;
import net.butfly.bus.hessian.SimpleHessianSkeleton;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.util.ServerWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.io.AbstractSerializerFactory;

public class BusHessianServlet extends SimpleHessianServlet {
	private static final long serialVersionUID = -2493158151789223720L;
	private static Logger logger = LoggerFactory.getLogger(BusHessianServlet.class);

	public BusHessianServlet() {
		super(new SimpleHessianSkeleton(EntryPoint.class));
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String factoryClasses = this.getInitParameter("factory-classes");
		if (null != factoryClasses) for (String className : factoryClasses.split(","))
			try {
				super.getSerializerFactory().addFactory((AbstractSerializerFactory) Class.forName(className).newInstance());
				logger.info("HessianSerializer servlet load serializer factory: " + className);
			} catch (Exception e) {
				logger.warn("HessianSerializer servlet load serializer factory failure: " + className);
			}
		try {
			logger.debug("Bus starting...");
			ServerWrapper servers = ServerWrapper.construct(this.getInitParameter("config-file"),
					this.getInitParameter("server-class"));
			Router router;
			try {
				router = (Router) Class.forName(this.getInitParameter("router-class")).newInstance();
			} catch (Throwable th) {
				router = new SimpleRouter();
			}
			super.putServiceTarget(new EntryPointImpl(servers, router));
			logger.info("Bus [" + this.getClass().getName() + "] started.");
		} catch (Throwable ex) {
			logger.error("Bus starting failed: ", ex);
			throw new ServletException(ex);
		}
	}
}
