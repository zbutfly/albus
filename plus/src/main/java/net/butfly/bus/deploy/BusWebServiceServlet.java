package net.butfly.bus.deploy;

import java.io.IOException;
import java.io.InputStream;
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

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.AsyncTask.AsyncCallback;
import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.bus.BasicBus;
import net.butfly.bus.argument.AsyncRequest;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.argument.TX;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.invoker.ParameterInfo;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.serialize.HTTPStreamingSupport;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.util.ServerWrapper;
import net.butfly.bus.util.TXUtils;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusWebServiceServlet extends BusServlet {
	private static final long serialVersionUID = 4533571572446977813L;
	private static Logger logger = LoggerFactory.getLogger(BusWebServiceServlet.class);
	private ServerWrapper servers;
	private Router router;
	private Map<String, Serializer> serializerMap;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		logger.trace("Bus starting...");
		servers = ServerWrapper.construct(this.getInitParameter("config-file"), this.getInitParameter("server-class"));
		try {
			router = (Router) Class.forName(this.getInitParameter("router-class")).newInstance();
		} catch (Throwable th) {
			router = new SimpleRouter();
		}
		String serializerClassnameList = this.getInitParameter("serializers");
		if (null == serializerClassnameList) this.createDefaultSerializerMap();
		else for (String cn : serializerClassnameList.split(","))
			try {
				Serializer inst = (Serializer) Thread.currentThread().getContextClassLoader().loadClass(cn).newInstance();
				for (ContentType ct : ((HTTPStreamingSupport) inst).getSupportedContentTypes())
					this.serializerMap.put(ct.getMimeType(), inst);
			} catch (Exception e) {}
		logger.info("Bus started.");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HeaderInfo info = this.header(request);
		Serializer serializer = this.serializerMap.get(ContentType.parse(request.getContentType()).getMimeType());
		if (serializer == null) throw new ServletException("Unmapped content type: " + request.getContentType());
		if (!((serializer instanceof HTTPStreamingSupport) && ((HTTPStreamingSupport) serializer).supportHTTPStream()))
			throw new ServletException("Unsupported content type: " + request.getContentType());
		response.setStatus(HttpStatus.SC_OK);
		response.setContentType(((HTTPStreamingSupport) serializer).getOutputContentType().toString());
		BasicBus server = this.router.route(info.tx.value(), servers.servers());
		ParameterInfo pi = server.getParameterInfo(info.tx.value(), info.tx.version());
		Object[] arguments = this.readFromBody(serializer, request.getInputStream(),
				pi.parametersTypes());
		AsyncCallback<Response> acb = new AsyncCallback<Response>() {
			@Override
			public void callback(Response r) {
				r.resultClass(pi.returnType());
				response.setHeader(HttpHeaders.ETAG, r.id());
				try {
					serializer.write(response.getOutputStream(), r);
					response.flushBuffer();
				} catch (IOException ex) {
					throw new SystemException("", ex);
				}
			}
		};
		Request req = new Request(info.tx, info.context, arguments);
		if (info.continuous) server.invoke(new AsyncRequest(req, acb));
		else acb.callback(server.invoke(req));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object[] readFromBody(Serializer serializer, InputStream is, Class<?>[] parameterTypes) throws ServletException,
			IOException {
		Object r = serializer.read(is, parameterTypes);
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
		String path = request.getPathInfo();
		String[] reses = null != path && path.length() > 0 ? path.substring(1).split("/") : new String[0];
		String code, version = null;
		switch (reses.length) {
		case 0: // anything in header.
			code = request.getHeader(BusHttpHeaders.HEADER_TX_CODE);
			version = request.getHeader(BusHttpHeaders.HEADER_TX_VERSION);
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
			if (name.startsWith(BusHttpHeaders.HEADER_CONTEXT_PREFIX))
				info.context.put(name.substring(BusHttpHeaders.HEADER_CONTEXT_PREFIX.length()), request.getHeader(name));
		}
		String cont = request.getHeader(BusHttpHeaders.HEADER_CONTINUOUS);
		info.continuous = null != cont && Boolean.parseBoolean(cont);
		return info;
	}

	protected static class HeaderInfo {
		TX tx;
		boolean continuous;
		Map<String, String> context;
	}

	private void createDefaultSerializerMap() {
		this.serializerMap = new HashMap<String, Serializer>();
		for (Class<? extends Serializer> clazz : ReflectionUtils.getSubClasses(Serializer.class, "")) {
			try {
				Serializer inst = clazz.newInstance();
				for (ContentType ct : ((HTTPStreamingSupport) inst).getSupportedContentTypes())
					this.serializerMap.put(ct.getMimeType(), inst);
			} catch (Exception e) {}
		}
	}
}
