package net.butfly.bus.invoker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Error;
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

import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
								+ KeyUtils.join(',', config.getTypeTranslators().toArray(new String[0])), e);
				logger.error("Invoker initialization continued but the factory is ignored.");
			}

		super.initialize(config, token);
	}

	private HttpHandler handler = new HttpUrlHandler(this.timeout, this.timeout);

	@Override
	public Task.Callable<Response> task(final Request request, final Options... remoteOptions) {
		return new Task.Callable<Response>() {
			@Override
			public Response call() throws Exception {
				Map<String, String> headers = HttpHandler.headers(request.code(), request.version(), request.context(),
						serializer.supportClass(), remoteOptions);
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
				HandlerResponse resp = WebServiceInvoker.this.handler.post(path, headers,
						serializer.serialize(request.arguments()), serializer.defaultMimeType(), serializer.charset(), false);

				Response response = new ResponseWrapper(resp.header(HttpHeaders.ETAG),
						resp.header(BusHttpHeaders.HEADER_REQUEST_ID));

				response.context(resp.parseContext());

				if (Boolean.parseBoolean(resp.header(BusHttpHeaders.HEADER_ERROR))) {
					Error detail = serializer.deserialize(resp.data, Error.class);
					response.error(detail);
				} else {
					String className = resp.header(BusHttpHeaders.HEADER_CLASS);
					Class<?> resultClass = className != null
							&& Boolean.parseBoolean(resp.header(BusHttpHeaders.HEADER_CLASS_SUPPORT)) ? Class
							.forName(className) : null;
					Object result = serializer.deserialize(resp.data, resultClass);
					response.result(result);
				}
				return ((ResponseWrapper) response).unwrap();
			}
		};
	}

	public static class HandlerResponse {
		private Map<String, List<String>> headers;
		private byte[] data;

		public HandlerResponse(Map<String, List<String>> headers, byte[] data) {
			super();
			this.headers = headers;
			this.data = data;
		}

		public Map<String, String> parseContext() {
			int prefixLen = BusHttpHeaders.HEADER_CONTEXT_PREFIX.length();
			Map<String, String> ctx = new HashMap<String, String>();
			if (headers == null || headers.size() == 0) return ctx;
			for (String name : headers.keySet())
				if (name != null && name.startsWith(BusHttpHeaders.HEADER_CONTEXT_PREFIX))
					ctx.put(name.substring(prefixLen), header(name));
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
