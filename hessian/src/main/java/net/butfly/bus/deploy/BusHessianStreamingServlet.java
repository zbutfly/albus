package net.butfly.bus.deploy;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.bus.comet.CometContext;
import net.butfly.bus.hessian.HessianExceptionHandler;
import net.butfly.bus.hessian.HessianSkeleton.Invoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.io.AbstractHessianOutput;

public class BusHessianStreamingServlet extends BusHessianServlet {
	private static final long serialVersionUID = -2493158151789223720L;
	private static Logger logger = LoggerFactory.getLogger(BusHessianStreamingServlet.class);
	private CometContext cometContext;

	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		super.service(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.cometContext = new CometContext(request, response);
		try {
			super.doPost(request, response);
		} catch (IOException ex) {
			logger.error("Bus service failed: ", ex);
			throw ex;
		} catch (ServletException ex) {
			logger.error("Bus service failed: ", ex);
			throw ex;
		} catch (Throwable ex) {
			logger.error("Bus service failed: ", ex);
			throw new ServletException(ex);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		cometContext.handleGet(request, response);
	}

	@Override
	protected void invoke(AbstractHessianOutput out, Invoker invoker, HessianExceptionHandler handler) throws IOException {
		while (true)
			super.invoke(out, invoker, handler);
	}

	@Override
	protected void handleStreamAfterInvoking(AbstractHessianOutput out) throws IOException {
		out.flush();
	}

	public static void initializeBusParameters(Object servlet) {
		try {
			Method method = servlet.getClass().getMethod("setInitParameter", String.class, String.class);
			if (null != method) method.invoke(servlet, "async-supported", "true");
		} catch (Exception e) {}
	}
}
