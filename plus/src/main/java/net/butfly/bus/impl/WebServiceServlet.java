package net.butfly.bus.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Bus;
import net.butfly.bus.Response;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.context.ResponseWrapper;
import net.butfly.bus.invoker.Invoking;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.serialize.Serializers;
import net.butfly.bus.utils.TXUtils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServiceServlet extends BusServlet implements Container<Servlet> {
	private static final long serialVersionUID = 4533571572446977813L;
	private static Logger logger = LoggerFactory.getLogger(WebServiceServlet.class);
	private Cluster cluster;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		logger.trace("Servlet starting...");
		String paramConfig = this.getInitParameter("config");
		this.cluster = new Cluster(null == paramConfig ? null : paramConfig.split(","), Bus.Mode.SERVER,
				this.parseRouterClasses(this.getInitParameter("router")));
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Router> parseRouterClasses(String className) {
		if (null == className) return SimpleRouter.class;
		try {
			return (Class<? extends Router>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			logger.warn("Router class invalid: " + className + ", default router used.", e);
			return SimpleRouter.class;
		}
	}

	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Headers", req.getHeader("Access-Control-Request-Headers"));
		resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException {
		// XXX: goof off
		this.doOptions(request, response);
		response.setStatus(HttpStatus.SC_OK);

		ContentType reqContentType = ContentType.parse(request.getContentType());

		// TODO: use reqContentType.getCharset() to construct serializer.
		final Serializer serializer = Serializers.serializer(reqContentType.getMimeType(), reqContentType.getCharset());
		if (serializer == null || Arrays.binarySearch(serializer.getSupportedMimeTypes(), reqContentType.getMimeType()) < 0)
			throw new ServletException("Unsupported content type: " + reqContentType.getMimeType());
		final ContentType respContentType = ContentType.create(serializer.getDefaultMimeType(), reqContentType.getCharset());
		final Invoking invoking = this.header(request, serializer);
		cluster.invoking(invoking);
		this.readFromBody(invoking, request.getInputStream(), serializer, reqContentType.getCharset());

		Response resp = cluster.invoke(invoking);
		response.setHeader(HttpHeaders.ETAG, resp.id());
		if (resp.context() != null) for (Entry<String, String> ctx : resp.context().entrySet())
			response.setHeader(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
		byte[] data = serializer.serialize(ResponseWrapper.wrap(resp, invoking.supportClass));
		logger.trace("HTTP Response SEND ==> " + new String(data, respContentType.getCharset()));
		response.getOutputStream().write(data);
		response.getOutputStream().flush();
		response.flushBuffer();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void readFromBody(Invoking invoking, InputStream is, Serializer serializer, Charset httpCharset)
			throws ServletException, IOException {
		byte[] data = IOUtils.toByteArray(is);
		if (logger.isTraceEnabled()) logger.trace("HTTP Request RECV <== " + new String(data, httpCharset));
		Object r = serializer.deserialize(data, invoking.parameterClasses);
		if (r == null) invoking.parameters = new Object[0];
		else if (r.getClass().isArray()) invoking.parameters = (Object[]) r;
		else if (Collection.class.isAssignableFrom(r.getClass())) {
			Collection c = Collection.class.cast(r);
			invoking.parameters = c.toArray(new Object[c.size()]);
		} else if (Iterable.class.isAssignableFrom(r.getClass())) {
			Iterable i = Iterable.class.cast(r);
			List l = new ArrayList();
			for (Iterator it = i.iterator(); it.hasNext();)
				l.add(it.next());
			invoking.parameters = l.toArray(new Object[l.size()]);
		} else if (Enumeration.class.isAssignableFrom(r.getClass())) {
			List l = new ArrayList();
			for (Enumeration e = Enumeration.class.cast(r); e.hasMoreElements();)
				l.add(e.nextElement());
			invoking.parameters = l.toArray(new Object[l.size()]);
		} else invoking.parameters = new Object[] { r };
	}

	protected Invoking header(HttpServletRequest request, Serializer serializer) throws ServletException {
		Invoking info = new Invoking();
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

}
