package net.butfly.bus.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.async.Callback;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Signal;
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
import net.butfly.bus.utils.async.ContinuousOptions;
import net.butfly.bus.utils.http.HttpHandler;
import net.butfly.bus.utils.http.HttpUrlHandler;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
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

	protected void invokeRemote(final Request request, final Callback<Response> callback, final Options options)
			throws IOException, Signal {
		if (null == callback) throw new IllegalArgumentException();

		if (options instanceof ContinuousOptions) {
			ContinuousOptions copts = (ContinuousOptions) options;
			Map<String, String> headers = this.header(request, copts);
			byte[] data = this.serializer.serialize(request.arguments());
			for (int i = 0; i < copts.retries(); i++)
				this.webservice(data, headers, callback, copts);
		} else callback.callback(this.invokeRemote(request, options));
	}

	@Override
	protected Response invokeRemote(final Request request, final Options options) throws IOException, Signal {
		Map<String, String> headers = this.header(request, options);
		byte[] data = this.serializer.serialize(request.arguments());
		return this.webservice(data, headers, null, options);
	}

	private Response webservice(byte[] data, Map<String, String> headers, Callback<Response> callback, Options options)
			throws IOException, Signal {
		InputStream http = null;
		Response resp = null;
		ContentType contentType = ((HTTPStreamingSupport) this.serializer).getOutputContentType();
		try {
			http = this.handler.post(this.path, headers, data, contentType, null != options
					&& options instanceof ContinuousOptions);
			do {
				byte[] recv = IOUtils.toByteArray(http);
				logger.trace("HTTP Response RECV <== " + new String(recv, contentType.getCharset()));
				Type c = this.serializer.supportClass() ? Response.class : ResponseWrapper.class;
				Response r = serializer.deserialize(recv, c);
				resp = this.convertResult(r);
				if (null == callback) return resp;
				callback.callback(resp);
			} while (resp != null);
		} finally {
			if (null != http) http.close();
		}
		return null;
	}

	private Response convertResult(Response resp) {
		if (null == resp) return null;
		// TODO: handle error in response
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

	private Map<String, String> header(Request request, Options options) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHttpHeaders.HEADER_TX_CODE, request.code());
		headers.put(BusHttpHeaders.HEADER_TX_VERSION, request.version());
		headers.put(BusHttpHeaders.HEADER_SUPPORT_CLASS, Boolean.toString(this.serializer.supportClass()));
		if (null != options) {
			headers.put(BusHttpHeaders.HEADER_CONTINUOUS, options.getClass().getName());
			headers.put(BusHttpHeaders.HEADER_CONTINUOUS_PARAMS, this.serializer.asString(options));
		}
		if (request.context() != null) for (Entry<String, String> ctx : request.context().entrySet())
			headers.put(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
		return headers;
	}
}
