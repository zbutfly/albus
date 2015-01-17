package net.butfly.bus.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Response;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.serialize.Serializers;
import net.butfly.bus.utils.http.HttpHandler;

import org.apache.commons.io.IOUtils;
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
		try {
			this.cluster = BusFactoryImpl.serverCluster(null == paramConfig ? null : paramConfig.split(","),
					this.getInitParameter("router"));
		} catch (ClassNotFoundException e) {
			throw new ServletException("Router definition not found.", e);
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

		final Serializer serializer = Serializers.serializer(reqContentType.getMimeType(), reqContentType.getCharset());
		if (serializer == null || Arrays.binarySearch(serializer.supportedMimeTypes(), reqContentType.getMimeType()) < 0)
			throw new ServletException("Unsupported content type: " + reqContentType.getMimeType());
		final ContentType respContentType = ContentType.create(serializer.defaultMimeType(), reqContentType.getCharset());

		Invoking invoking = new Invoking();
		Map<String, String> busHeaders = HttpHandler.headers(request);
		invoking.context = HttpHandler.context(busHeaders);
		invoking.tx = HttpHandler.tx(request.getPathInfo(), busHeaders);
		invoking.options = busHeaders.containsKey(BusHttpHeaders.HEADER_OPTIONS) ? (Options) serializer.fromString(
				busHeaders.get(BusHttpHeaders.HEADER_OPTIONS), Options.class) : null;
		invoking.supportClass = Boolean.parseBoolean(busHeaders.get(BusHttpHeaders.HEADER_CLASS_SUPPORT));
		cluster.invoking(invoking);
		invoking.parameters = HttpHandler.parameters(IOUtils.toByteArray(request.getInputStream()), serializer,
				invoking.parameterClasses, reqContentType.getCharset());

		Response resp = cluster.invoke(invoking);
		response.getOutputStream().write(
				HttpHandler.response(resp, response, serializer, invoking.supportClass, respContentType.getCharset()));
		response.getOutputStream().flush();
		response.flushBuffer();
	}
}
