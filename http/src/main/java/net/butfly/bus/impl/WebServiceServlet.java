package net.butfly.bus.impl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.butfly.albacore.serializer.Serializers;
import net.butfly.albacore.serializer.TextSerializer;
import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.Instances;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.async.Opts;
import net.butfly.albacore.utils.async.Task;
import net.butfly.albacore.utils.async.Task.Callback;
import net.butfly.bus.Response;
import net.butfly.bus.context.Context;
import net.butfly.bus.policy.Router;
import net.butfly.bus.utils.http.BusHeaders;
import net.butfly.bus.utils.http.HttpHandler;
import net.butfly.bus.utils.http.MoreOpts;

public class WebServiceServlet extends BusServlet {
	private static final long serialVersionUID = 4533571572446977813L;
	protected static Logger logger = LoggerFactory.getLogger(WebServiceServlet.class);

	protected Cluster cluster;
	protected Opts opts;
	protected boolean internalAsync;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String paramConfig = this.getInitParameter("config");
		logger.info("Servlet [" + paramConfig + "] starting...");
		Class<? extends Router> routerClass = Reflections.forClassName(this.getInitParameter("router"));
		this.cluster = BusFactory.serverCluster(routerClass, null == paramConfig ? new String[0] : paramConfig.split(","));
		this.internalAsync = AsyncCluster.class.isAssignableFrom(this.cluster.getClass());
		this.opts = new MoreOpts();
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
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final ServiceContext context = this.prepare(request, response);
		Callback<Response> cb = new Task.Callback<Response>() {
			@Override
			public void callback(Response resp) {
				try {
					response.getOutputStream().write(context.handler.response(resp, response, context.invoking.supportClass,
							context.respContentType.getCharset()));
					response.getOutputStream().flush();
					response.flushBuffer();
				} catch (IOException ex) {
					logger.error("Response writing I/O failure", ex);
				}
			}
		};
		try {
			if (this.internalAsync) ((AsyncCluster) cluster).invoke(context.invoking, cb);
			else cb.callback(cluster.invoke(context.invoking));
		} catch (Exception ex) {
			throw Exceptions.wrap(ex, ServletException.class);
		}
	}

	protected ServiceContext prepare(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
		// XXX: goof off
		this.doOptions(request, response);
		response.setStatus(HttpStatus.OK_200);

		ServiceContext context = new ServiceContext();

		context.reqContentType = ContentType.parse(request.getContentType());
		if (context.reqContentType.getCharset() == null) context.reqContentType = context.reqContentType.withCharset(
				Serializers.DEFAULT_CONTENT_TYPE.getCharset());
		if (context.reqContentType.getMimeType() == null) context.reqContentType = ContentType.create(Serializers.DEFAULT_CONTENT_TYPE
				.getMimeType(), context.reqContentType.getCharset());
		final TextSerializer serializer = (TextSerializer) Serializers.serializer(Serializers.serializerClass(context.reqContentType
				.getMimeType()), context.reqContentType.getCharset());
		if (serializer == null) throw new ServletException("Unsupported mime type: " + context.reqContentType.getMimeType());
		context.respContentType = ContentType.create(serializer.contentType().getMimeType(), context.reqContentType.getCharset());
		context.handler = Instances.fetch(HttpHandler.class, serializer);

		// prepare invoke
		context.invoking = new Invoking();
		Map<String, String> busHeaders = context.handler.headers(request);
		context.invoking.context = context.handler.context(busHeaders);
		context.invoking.context.put(Context.Key.SourceHost.name(), context.handler.source(request));
		context.invoking.tx = context.handler.tx(request.getPathInfo(), busHeaders);
		context.invoking.options = busHeaders.containsKey(BusHeaders.HEADER_OPTIONS) ? this.opts.parses(busHeaders.get(
				BusHeaders.HEADER_OPTIONS)) : null;

		context.invoking.supportClass = Boolean.parseBoolean(busHeaders.get(BusHeaders.HEADER_CLASS_SUPPORT));
		cluster.invoking(context.invoking);
		byte[] paramsData;
		try {
			paramsData = IOUtils.toByteArray(request.getInputStream());
		} catch (IOException ex) {
			throw new ServletException("Arguments reading I/O failure", ex);
		}
		context.invoking.parameters = context.handler.parameters(paramsData, context.reqContentType.getCharset(),
				context.invoking.parameterClasses);
		return context;
	}

	protected class ServiceContext {
		protected ContentType reqContentType, respContentType;
		protected Invoking invoking;
		protected HttpHandler handler;
	}
}
