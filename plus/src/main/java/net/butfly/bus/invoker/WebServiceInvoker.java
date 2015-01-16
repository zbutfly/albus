package net.butfly.bus.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.Token;
import net.butfly.bus.config.invoker.WebServiceInvokerConfig;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.serialize.SerializerFactorySupport;
import net.butfly.bus.serialize.Serializers;
import net.butfly.bus.utils.http.HttpHandler;
import net.butfly.bus.utils.http.HttpUrlHandler;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class WebServiceInvoker extends AbstractRemoteInvoker<WebServiceInvokerConfig> implements
		Invoker<WebServiceInvokerConfig> {
	private static Logger logger = LoggerFactory.getLogger(WebServiceInvoker.class);
	private static int DEFAULT_TIMEOUT = 5000;
	private String path;
	private int timeout;

	private Serializer serializer;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(WebServiceInvokerConfig config, Token token) {
		this.path = config.getPath();
		this.timeout = config.getTimeout() > 0 ? config.getTimeout() : DEFAULT_TIMEOUT;
		try {
			this.serializer = Serializers.serializer((Class<? extends Serializer>) Class.forName(config.getSerializer()));
		} catch (Exception e) {
			this.serializer = Serializers.serializer();
		}
		if (this.serializer instanceof SerializerFactorySupport)
			try {
				((SerializerFactorySupport) this.serializer).addFactoriesByClassName(config.getTypeTranslators());
			} catch (Exception e) {
				logger.error(
						"Serializer factory instance construction failure for class: "
								+ KeyUtils.join(config.getTypeTranslators().toArray(new String[0])), e);
				logger.error("Invoker initialization continued but the factory is ignored.");
			}

		super.initialize(config, token);
	}

	private HttpHandler handler = new HttpUrlHandler(this.timeout, this.timeout);

	@Override
	protected Task.Callable<Response> task(Request request, Options[] options) {
		return new InvokeTask(request, this.remoteOptions(options));
	}

	private class InvokeTask extends Task.Callable<Response> {
		private Request request;
		private Options[] remoteOptions;

		InvokeTask(Request request, Options... remoteOptions) {
			this.request = request;
			this.remoteOptions = remoteOptions;
		}

		@Override
		public Response call() throws Exception {
			Map<String, String> headers = header(request, remoteOptions);
			byte[] data = serializer.serialize(request.arguments());
			ContentType contentType = ContentType.create(serializer.getDefaultMimeType(), Charsets.UTF_8);
			/**
			 * <pre>
			 * TODO: handle continuous, move to async proj.
			 * 		if (remoteOptions instanceof ContinuousOptions) {
			 * 			ContinuousOptions copts = (ContinuousOptions) remoteOptions;
			 * 			Map&lt;String, String&gt; headers = this.header(request, copts);
			 * 			byte[] data = this.serializer.serialize(request.arguments());
			 * 			for (int i = 0; i &lt; copts.retries(); i++)
			 * 				this.webservice(data, headers, callback, copts);
			 *  } else
			 * </pre>
			 */
			HandlerResponse resp = WebServiceInvoker.this.handler.post(path, headers, data, contentType, false);

			Response response = new ResponseWrapper(resp.header(HttpHeaders.ETAG),
					resp.header(BusHttpHeaders.HEADER_REQUEST_ID));

			response.context(resp.parseContext());

			boolean error = Boolean.parseBoolean(resp.header(BusHttpHeaders.HEADER_ERROR));
			if (error) {
				net.butfly.bus.Error detail = serializer.fromString(resp.header(BusHttpHeaders.HEADER_ERROR_DETAIL),
						net.butfly.bus.Error.class);
				response.error(detail);
			}
			boolean supportClass = Boolean.parseBoolean(resp.header(BusHttpHeaders.HEADER_CLASS_SUPPORT));
			String className = resp.header(BusHttpHeaders.HEADER_CLASS);
			Class<?> resultClass = supportClass && className != null ? Class.forName(className) : null;
			byte[] recv = IOUtils.toByteArray(resp.input);
			if (logger.isTraceEnabled()) logger.trace("HTTP Response RECV <== " + new String(recv, contentType.getCharset()));
			Object result = serializer.deserialize(recv, resultClass);
			response.result(result);
			return ((ResponseWrapper) response).unwrap();
		}
	}

	private Map<String, String> header(Request request, Options... options) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHttpHeaders.HEADER_TX_CODE, request.code());
		headers.put(BusHttpHeaders.HEADER_TX_VERSION, request.version());
		headers.put(BusHttpHeaders.HEADER_CLASS_SUPPORT, Boolean.toString(this.serializer.supportClass()));
		if (null != options && options.length > 0) {
			headers.put(BusHttpHeaders.HEADER_OPTIONS, this.serializer.asString(options));
		}
		if (request.context() != null) for (Entry<String, String> ctx : request.context().entrySet())
			headers.put(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
		return headers;
	}

	public static class HandlerResponse {
		private Map<String, List<String>> headers;
		private InputStream input;

		public HandlerResponse(Map<String, List<String>> headers, InputStream input) {
			super();
			this.headers = headers;
			this.input = input;
		}

		public Map<String, String> parseContext() {
			int prefixLen = BusHttpHeaders.HEADER_CONTEXT_PREFIX.length();
			Map<String, String> ctx = new HashMap<String, String>();
			if (headers == null || headers.size() == 0) return ctx;
			for (String name : headers.keySet())
				if (name.startsWith(BusHttpHeaders.HEADER_CONTEXT_PREFIX)) ctx.put(name.substring(prefixLen), header(name));
			return ctx;
		}

		private String header(String name) {
			if (headers == null || headers.size() == 0) return null;
			List<String> values = headers.get(name);
			if (null == values || values.size() == 0) return null;
			return values.get(0);
		}
	}

	private static class ResponseWrapper extends Response {
		private static final long serialVersionUID = 37612994599685817L;

		public ResponseWrapper(String id, String requestId) {
			super();
			this.id = id;
			this.requestId = requestId;
		}

		public Response unwrap() {
			return this;
		}
	}
}
