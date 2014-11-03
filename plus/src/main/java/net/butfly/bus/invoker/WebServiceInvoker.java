package net.butfly.bus.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.argument.AsyncRequest;
import net.butfly.bus.argument.Request;
import net.butfly.bus.argument.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.invoker.WebServiceInvokerConfig;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.serialize.HTTPStreamingSupport;
import net.butfly.bus.serialize.JSONSerializer;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.serialize.SerializerFactorySupport;
import net.butfly.bus.utils.http.HttpHandler;
import net.butfly.bus.utils.http.HttpUrlHandler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServiceInvoker extends AbstractRemoteInvoker<WebServiceInvokerConfig> implements
		Invoker<WebServiceInvokerConfig> {
	private static Logger logger = LoggerFactory.getLogger(WebServiceInvoker.class);
	private static int DEFAULT_TIMEOUT = 5000;
	private String path;
	private int timeout;

	private Serializer serializer;

	@Override
	public void initialize(WebServiceInvokerConfig config, Token token) {
		this.path = config.getPath();
		this.timeout = config.getTimeout() > 0 ? config.getTimeout() : DEFAULT_TIMEOUT;
		try {
			this.serializer = (Serializer) Class.forName(config.getSerializer()).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
			this.serializer = new JSONSerializer();
		}
		if (!(this.serializer instanceof HTTPStreamingSupport) || !((HTTPStreamingSupport) this.serializer).supportHTTPStream())
			throw new SystemException("", "Serializer should support HTTP streaming mode.");
		if (this.serializer instanceof SerializerFactorySupport)
			try {
				((SerializerFactorySupport) this.serializer).addFactoriesByClassName(config.getTypeTranslators());
			} catch (Exception e) {
				logger.error(
						"Serializer factory instance construction failure for class: "
								+ StringUtils.join(config.getTypeTranslators().toArray(new String[0])), e);
				logger.error("Invoker initialization continued but the factory is ignored.");
			}

		super.initialize(config, token);
	}

	private HttpHandler handler = new HttpUrlHandler(this.timeout, this.timeout);

	protected void asyncInvoke(AsyncRequest request) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHttpHeaders.HEADER_TX_CODE, request.code());
		headers.put(BusHttpHeaders.HEADER_TX_VERSION, request.version());
		headers.put(BusHttpHeaders.HEADER_CONTINUOUS, Boolean.toString(request.retries() >= 0));
		if (request.context() != null) for (Entry<String, String> ctx : request.context().entrySet())
			headers.put(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());

		PipedOutputStream os = new PipedOutputStream();
		PipedInputStream is = new PipedInputStream(os);
		this.serializer.write(os, request.arguments());
		os.close();
		do {
			InputStream resp = this.handler.post(this.path, is,
					((HTTPStreamingSupport) this.serializer).getOutputContentType(), headers, true);
			while (true) {
				Response r = this.serializer.read(resp, Response.class);
				request.callback().callback(this.convertResult(r));
				if (r == null) break;
			}
		} while (request.retry());
	}

	private Response convertResult(Response resp) {
		Object r = resp.result();
		Class<?> expected = resp.resultClass();
		if (null != resp && null != expected && null != r && !expected.isAssignableFrom(r.getClass())) {
			PipedOutputStream os = new PipedOutputStream();
			try {
				PipedInputStream is = new PipedInputStream(os);
				this.serializer.write(os, r);
				os.close();
				r = this.serializer.read(is, expected);
			} catch (IOException ex) {}
			resp.result(r);
		}
		return resp;
	}

	protected Response syncInvoke(Request request) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHttpHeaders.HEADER_TX_CODE, request.code());
		headers.put(BusHttpHeaders.HEADER_TX_VERSION, request.version());
		headers.put(BusHttpHeaders.HEADER_CONTINUOUS, "false");
		if (request.context() != null) for (Entry<String, String> ctx : request.context().entrySet())
			headers.put(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());

		PipedOutputStream os = new PipedOutputStream();
		PipedInputStream is = new PipedInputStream(os);
		this.serializer.write(os, request.arguments());
		os.close();
		InputStream resp = this.handler.post(this.path, is, ((HTTPStreamingSupport) this.serializer).getOutputContentType(),
				headers, false);
		Response r = this.serializer.read(resp, Response.class);
		resp.close();
		return this.convertResult(r);
	}
}
