package net.butfly.bus.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.ExceptionUtils;
import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.Token;
import net.butfly.bus.config.invoker.WebServiceInvokerConfig;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.context.ResponseWrapper;
import net.butfly.bus.serialize.HTTPStreamingSupport;
import net.butfly.bus.serialize.JSONSerializer;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.serialize.SerializerFactorySupport;
import net.butfly.bus.utils.BusTask;
import net.butfly.bus.utils.http.HttpHandler;
import net.butfly.bus.utils.http.HttpUrlHandler;

import org.apache.commons.io.IOUtils;
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
								+ KeyUtils.join(config.getTypeTranslators().toArray(new String[0])), e);
				logger.error("Invoker initialization continued but the factory is ignored.");
			}

		super.initialize(config, token);
	}

	private HttpHandler handler = new HttpUrlHandler(this.timeout, this.timeout);

	@Override
	protected Response invokeRemote(final Request request, final Options... options) throws IOException {
		Map<String, String> headers = this.header(request, this.remoteOptions(options));
		byte[] data = this.serializer.serialize(request.arguments());
		return this.webservice(data, headers, null, this.localOptions(options));
	}

	@Override
	protected void invokeRemote(final Request request, final Task.Callback<Response> callback, final Options... options)
			throws IOException {
		Map<String, String> headers = this.header(request, this.remoteOptions(options));
		byte[] data = this.serializer.serialize(request.arguments());
		this.webservice(data, headers, callback, this.localOptions(options));
	}

	private Response webservice(final byte[] data, final Map<String, String> headers, final Task.Callback<Response> callback,
			Options options) throws IOException {
		Task<Response> task = new BusTask<Response>(new Task.Callable<Response>() {
			@Override
			public Response call() throws Exception {
				InputStream http = null;
				ContentType contentType = ((HTTPStreamingSupport) serializer).getOutputContentType();
				/**
				 * <pre>
				 * TODO: handle continuous, move to async proj.
				 * 		if (options instanceof ContinuousOptions) {
				 * 			ContinuousOptions copts = (ContinuousOptions) options;
				 * 			Map&lt;String, String&gt; headers = this.header(request, copts);
				 * 			byte[] data = this.serializer.serialize(request.arguments());
				 * 			for (int i = 0; i &lt; copts.retries(); i++)
				 * 				this.webservice(data, headers, callback, copts);
				 *  } else
				 * </pre>
				 */
				http = handler.post(path, headers, data, contentType, false);
				byte[] recv = IOUtils.toByteArray(http);
				logger.trace("HTTP Response RECV <== " + new String(recv, contentType.getCharset()));
				return convertResult(recv);
			}
		}, callback, options);
		try {
			return task.execute();
		} catch (Exception e) {
			throw ExceptionUtils.wrap(e);
		}
	}

	private Response convertResult(byte[] recv) {
		Response resp = serializer.deserialize(recv, ResponseWrapper.wrapClass(serializer.supportClass()));
		if (null == resp) return null;
		Object result = resp.result();
		if (null == result) return resp;
		Type expected = ResponseWrapper.unwrap(resp);
		if (null != expected) resp.result(this.serializer.deserialize(this.serializer.serialize(result), expected));
		return resp;
	}

	private Map<String, String> header(Request request, Options... options) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHttpHeaders.HEADER_TX_CODE, request.code());
		headers.put(BusHttpHeaders.HEADER_TX_VERSION, request.version());
		headers.put(BusHttpHeaders.HEADER_SUPPORT_CLASS, Boolean.toString(this.serializer.supportClass()));
		if (null != options && options.length > 0) {
			headers.put(BusHttpHeaders.HEADER_OPTIONS, this.serializer.asString(options));
		}
		if (request.context() != null) for (Entry<String, String> ctx : request.context().entrySet())
			headers.put(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
		return headers;
	}
}
