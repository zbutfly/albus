package net.butfly.bus.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.argument.ResponseWrapper;
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
			this.serializer = (Serializer) Class.forName(config.getSerializer()).getConstructor(String[].class)
					.newInstance(config.getTypeTranslators());
		} catch (Exception e) {
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

//	protected void continuousInvoke(Request request, Options options) throws IOException {
//		Map<String, String> headers = this.getHeaders(request, true);
//		byte[] data = this.serializer.serialize(request.arguments());
//		do {
//			InputStream http = this.handler.post(this.path, data,
//					((HTTPStreamingSupport) this.serializer).getOutputContentType(), headers, true);
//			while (true) {
//				Response r = this.serializer.supportClass() ? this.serializer.read(http, Response.class) : this.serializer
//						.read(http, ResponseWrapper.class);
//				options.callback().callback(this.convertResult(r));
//				if (r == null) break;
//			}
//			http.close();
//		} while (request.retry());
//	}

	protected Response singleInvoke(Request request) throws IOException {
		Map<String, String> headers = this.getHeaders(request, false);
		InputStream http = this.handler.post(this.path, this.serializer.serialize(request.arguments()),
				((HTTPStreamingSupport) this.serializer).getOutputContentType(), headers, false);
		Response r = this.serializer.supportClass() ? this.serializer.read(http, Response.class) : this.serializer.read(http,
				ResponseWrapper.class);
		http.close();
		return this.convertResult(r);
	}

	private Response convertResult(Response resp) {
		Object r = resp.result();
		if (null != r && resp instanceof ResponseWrapper) {
			Type expected = ((ResponseWrapper) resp).resultClass();
			if (null != expected) {
				r = this.serializer.deserialize(this.serializer.serialize(r), expected);
				resp.result(r);
			}
		}
		return resp;
	}

	private Map<String, String> getHeaders(Request request, boolean streaming) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHttpHeaders.HEADER_TX_CODE, request.code());
		headers.put(BusHttpHeaders.HEADER_TX_VERSION, request.version());
		headers.put(BusHttpHeaders.HEADER_SUPPORT_CLASS, Boolean.toString(this.serializer.supportClass()));
		headers.put(BusHttpHeaders.HEADER_CONTINUOUS, Boolean.toString(streaming));
		if (request.context() != null) for (Entry<String, String> ctx : request.context().entrySet())
			headers.put(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
		return headers;
	}

}
