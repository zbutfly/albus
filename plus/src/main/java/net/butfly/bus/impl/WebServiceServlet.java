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
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Response;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.context.Context;
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
		String paramConfig = this.getInitParameter("config");
		logger.info("Servlet [" + paramConfig + "] starting...");
		try {
			this.cluster = BusFactory.serverCluster(this.getInitParameter("router"),
					null == paramConfig ? null : paramConfig.split(","));
		} catch (ClassNotFoundException e) {
			throw new ServletException("Router definition not found.", e);
		}
		// FOR debug
		if (Boolean.parseBoolean(System.getProperty("bus.server.waiting"))) System.setProperty("bus.server.waiting", "false");
		logger.info("Servlet [" + paramConfig + "] started.");
	}

	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Headers", req.getHeader("Access-Control-Request-Headers"));
		resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
		// XXX: goof off
		this.doOptions(request, response);
		response.setStatus(HttpStatus.SC_OK);
		ContentType reqContentType = ContentType.parse(request.getContentType());
		if (reqContentType.getCharset() == null)
			reqContentType = ContentType.create(reqContentType.getMimeType(), Serializers.DEFAULT_CHARSET);
		if (reqContentType.getMimeType() == null)
			reqContentType = ContentType.create(Serializers.DEFAULT_MIME_TYPE, reqContentType.getCharset());
		final Serializer serializer = Serializers.serializer(reqContentType.getMimeType(), reqContentType.getCharset());
		if (serializer == null || Arrays.binarySearch(serializer.supportedMimeTypes(), reqContentType.getMimeType()) < 0)
			throw new ServletException("Unsupported content type: " + reqContentType.getMimeType());
		final ContentType respContentType = ContentType.create(serializer.defaultMimeType(), reqContentType.getCharset());

		final Invoking invoking = new Invoking();
		Map<String, String> busHeaders = HttpHandler.headers(request);
		invoking.context = HttpHandler.context(busHeaders);
		invoking.context.put(Context.Key.SourceHost.name(), request.getRemoteAddr());
		invoking.tx = HttpHandler.tx(request.getPathInfo(), busHeaders);
		if (!busHeaders.containsKey(BusHttpHeaders.HEADER_OPTIONS)) invoking.options = null;
		else {
			String[] opstrs = busHeaders.get(BusHttpHeaders.HEADER_OPTIONS).split("\\|");
			invoking.options = new Options[opstrs.length];
			for (int i = 0; i < opstrs.length; i++)
				invoking.options[i] = new Options(opstrs[i]);
		}

		invoking.supportClass = Boolean.parseBoolean(busHeaders.get(BusHttpHeaders.HEADER_CLASS_SUPPORT));
		cluster.invoking(invoking);
		byte[] paramsData;
		try {
			paramsData = IOUtils.toByteArray(request.getInputStream());
		} catch (IOException ex) {
			throw new ServletException("Arguments reading I/O failure", ex);
		}
		invoking.parameters = HttpHandler.parameters(paramsData, serializer, invoking.parameterClasses,
				reqContentType.getCharset());
		cluster.invoke(invoking, new Task.Callback<Response>() {
			@Override
			public void callback(Response resp) {
				try {
					response.getOutputStream().write(
							HttpHandler.response(resp, response, serializer, invoking.supportClass,
									respContentType.getCharset()));
					response.getOutputStream().flush();
					response.flushBuffer();
				} catch (IOException ex) {
					logger.error("Response writing I/O failure", ex);
				}
			}
		});
	}
}
