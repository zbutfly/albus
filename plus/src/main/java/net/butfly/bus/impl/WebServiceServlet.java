package net.butfly.bus.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.ReflectionUtils;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.ResponseWrapper;
import net.butfly.bus.invoker.ParameterInfo;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.serialize.HTTPStreamingSupport;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.utils.BusTask;
import net.butfly.bus.utils.TXUtils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServiceServlet extends BusServlet {
	private static final long serialVersionUID = 4533571572446977813L;
	private static Logger logger = LoggerFactory.getLogger(WebServiceServlet.class);
	private Cluster cluster;
	private Router router;
	private Map<String, Serializer> serializerMap;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		logger.trace("Servlet starting...");
		cluster = new Cluster(this.getInitParameter("config-file"), BusMode.SERVER);
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

	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Headers", req.getHeader("Access-Control-Request-Headers"));
		resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
	}

	@Override
	protected void doPost(HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final Serializer serializer = this.serializerMap.get(ContentType.parse(request.getContentType()).getMimeType());
		final HeaderInfo info = this.header(request, serializer);
		if (serializer == null) throw new ServletException("Unmapped content type: " + request.getContentType());
		if (!((serializer instanceof HTTPStreamingSupport) && ((HTTPStreamingSupport) serializer).supportHTTPStream()))
			throw new ServletException("Unsupported content type: " + request.getContentType());
		response.setStatus(HttpStatus.SC_OK);
		// XXX: goof off
		this.doOptions(request, response);
		final ContentType respContentType = ((HTTPStreamingSupport) serializer).getOutputContentType();
		final Bus bus = this.router.route(info.tx.value(), (BusImpl[]) cluster.servers());
		if (null == bus) throw new SystemException("", "Server routing failure.");
		ParameterInfo pi = ((BusImpl) bus).getParameterInfo(info.tx.value(), info.tx.version());
		if (null == pi) throw new SystemException("", "Server routing failure.");
		Object[] arguments = this.readFromBody(serializer, request.getInputStream(), pi.parametersTypes());
		final Request req = new Request(info.tx, info.context, arguments);
		Task.Callback<Response> callback = new Task.Callback<Response>() {
			@Override
			public void callback(Response r) throws Exception {
				response.setHeader(HttpHeaders.ETAG, r.id());
				if (r.context() != null) for (Entry<String, String> ctx : r.context().entrySet())
					response.setHeader(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
				byte[] data = serializer.serialize(ResponseWrapper.wrap(r, info.supportClass));
				logger.trace("HTTP Response SEND ==> " + new String(data, respContentType.getCharset()));
				response.getOutputStream().write(data);
				response.getOutputStream().flush();
				response.flushBuffer();
			}
		};
		Task.Callable<Response> task = new Task.Callable<Response>() {
			@Override
			public Response call() throws Exception {
				return ((BusImpl) bus).invoke(req, info.options);
			}
		};
		Context.initialize(Context.deserialize(req.context()));
		try {
			new BusTask<Response>(task, callback).execute();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		logger.info("Servlet releasing.");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object[] readFromBody(Serializer serializer, InputStream is, Class<?>[] parameterTypes) throws ServletException,
			IOException {
		byte[] data = IOUtils.toByteArray(is);
		logger.trace("HTTP Request RECV <== "
				+ new String(data, ((HTTPStreamingSupport) serializer).getOutputContentType().getCharset()));
		Object r = serializer.deserialize(data, parameterTypes);
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

	protected HeaderInfo header(HttpServletRequest request, Serializer serializer) throws ServletException {
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
		String h = request.getHeader(BusHttpHeaders.HEADER_OPTIONS);
		info.options = null == h ? null : (Options) serializer.fromString(h, Options.class);
		String supportClass = request.getHeader(BusHttpHeaders.HEADER_SUPPORT_CLASS);
		info.supportClass = null == supportClass || Boolean.parseBoolean(supportClass);
		return info;
	}

	protected static class HeaderInfo {
		TX tx;
		boolean supportClass;
		Options options;
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
