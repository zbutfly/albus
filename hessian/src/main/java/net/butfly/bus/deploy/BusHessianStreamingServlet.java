package net.butfly.bus.deploy;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
			try {
				super.invoke(out, invoker, handler);
			} catch (EOFException ex) {
				logger.warn("Connection lost.");
				break;
			}
	}

	@Override
	protected void handleStreamAfterInvoking(AbstractHessianOutput out) throws IOException {
		out.flush();
	}

	@ServletInitParams private static Map<String, String> params;
	static {
		params = new HashMap<String, String>();
		params.put("async-supported", "true");
	}
}
