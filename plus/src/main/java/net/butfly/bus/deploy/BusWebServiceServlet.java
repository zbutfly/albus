package net.butfly.bus.deploy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.albacore.utils.serialize.HessianSerializer;
import net.butfly.albacore.utils.serialize.Serializer;
import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.ServerWrapper;
import net.butfly.bus.TX;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.util.TXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusWebServiceServlet extends BusServlet {
	private static final long serialVersionUID = 4533571572446977813L;
	private static Logger logger = LoggerFactory.getLogger(BusWebServiceServlet.class);
	private Serializer serializer;
	private ServerWrapper servers;
	private Router router;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			logger.trace("Bus starting...");
			servers = ServerWrapper.construct(this.getInitParameter("config-file"),
					this.getInitParameter("server-class"));
			try {
				router = (Router) Class.forName(this.getInitParameter("router-class")).newInstance();
			} catch (Throwable th) {
				router = new SimpleRouter();
			}
			try {
				serializer = (Serializer) Class.forName(this.getInitParameter("serializer")).newInstance();
			} catch (Throwable th) {
				serializer = new HessianSerializer();
			}
			logger.info("Bus started.");
		} catch (Throwable ex) {
			logger.error("Bus starting failed: ", ex);
			throw new ServletException(ex);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		HeaderInfo info = this.header(request);
		response.setStatus(200);
		response.setContentType("application/json; charset=utf-8");
		Bus server = this.router.route(info.tx.value(), servers.servers());
		Object[] arguments = this.readFromBody(request, server.getParameterTypes(info.tx.value(), info.tx.version()));
		Response r = server.invoke(new Request(info.tx, info.context, arguments));
		response.setHeader("ETag", r.id());
		if (serializer.supportHTTPStream())
			serializer.write(response.getOutputStream(), r);
		else serializer.write(response.getWriter(), r);
		response.flushBuffer();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object[] readFromBody(HttpServletRequest request, Class<?>[] parameterTypes) throws ServletException,
			IOException {
		Object r = serializer.supportHTTPStream() ? serializer.read(request.getInputStream(), parameterTypes)
				: serializer.read(request.getReader(), parameterTypes);
		if (r == null) return null;
		if (r.getClass().isArray()) return (Object[]) r;
		if (Collection.class.isAssignableFrom(r.getClass())) {
			Collection c = Collection.class.cast(r);
			return c.toArray(new Object[c.size()]);
		}
		if (Iterable.class.isAssignableFrom(r.getClass())) {
			Iterable i = Iterable.class.cast(r);
			List l = new ArrayList();
			for (Iterator it = i.iterator(); it.hasNext();)
				l.add(it.next());
			return l.toArray(new Object[l.size()]);
		}
		if (Enumeration.class.isAssignableFrom(r.getClass())) {
			List l = new ArrayList();
			for (Enumeration e = Enumeration.class.cast(r); e.hasMoreElements();)
				l.add(e.nextElement());
			return l.toArray(new Object[l.size()]);
		}
		return new Object[] { r };
	}

	protected HeaderInfo header(HttpServletRequest request) throws ServletException {
		HeaderInfo info = new HeaderInfo();
		info.context = new HashMap<String, String>();
		String[] reses = request.getPathInfo().substring(1).split("/");
		String code, version = null;
		switch (reses.length) {
		case 0: // anything in header.
			code = request.getHeader("X-BUS-TX");
			version = request.getHeader("X-BUS-TX-Version");
			break;
		case 2:
			version = reses[1];
		case 1:
			code = reses[0];
			break;
		default:
			throw new ServletException("Invalid path: " + request.getPathInfo());
		}
		info.tx = TXUtils.TXImpl(code, version);

		for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
			String name = e.nextElement();
			if (name.startsWith("X-BUS-Context-")) {
				String value = request.getHeader(name);
				info.context.put(name, value);
			}
		}
		return info;
	}

	protected static class HeaderInfo {
		TX tx;
		Map<String, String> context;
	}
}
