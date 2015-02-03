package net.butfly.bus.invoker;

import java.util.Map;

import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.Instances;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.Texts;
import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.serialize.SerializerFactorySupport;
import net.butfly.bus.serialize.Serializers;
import net.butfly.bus.utils.http.HttpHandler;
import net.butfly.bus.utils.http.HttpNingHandler;
import net.butfly.bus.utils.http.ResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServiceInvoker extends AbstractRemoteInvoker implements Invoker {
	private static Logger logger = LoggerFactory.getLogger(WebServiceInvoker.class);
	private String path;
	private int timeout;

	private Serializer serializer;
	private HttpHandler handler;

	@Override
	public void initialize(InvokerConfig config, Token token) {
		this.path = config.param("path");
		String to = config.param("timeout");
		this.timeout = to == null ? 0 : Integer.parseInt(to);
		try {
			Class<? extends Serializer> cl = Reflections.forClassName(config.param("serializer"));
			this.serializer = Serializers.serializer(cl, Serializers.DEFAULT_CHARSET);
		} catch (Exception e) {
			logger.error("Invoker initialization failure, Serializer could not be created.", e);
			throw Exceptions.wrap(e);
		}
		if (this.serializer instanceof SerializerFactorySupport) {
			String[] trs = config.param("typeTranslators") == null ? new String[0] : config.param("typeTranslators").split(",");
			try {
				((SerializerFactorySupport) this.serializer).addFactoriesByClassName(trs);
			} catch (Exception e) {
				logger.error("Serializer factory instance construction failure for class: " + Texts.join(',', trs), e);
				logger.error("Invoker initialization continued but the factory is ignored.");
			}
		}
		final String handleClassname = config.param("handler");
		final Class<? extends HttpHandler> handlerClass;
		if (null == handleClassname) handlerClass = HttpNingHandler.class;// (serializer, timeout, timeout);
		else handlerClass = Reflections.forClassName(handleClassname);
		try {
			this.handler = Instances.fetch(new HttpHandler.Instantiator(handlerClass, serializer, timeout, timeout),
					handlerClass, serializer, timeout, timeout);
		} catch (Exception e) {
			throw Exceptions.wrap(e);
		}
		super.initialize(config, token);
	}

	// new HttpUrlHandler(this.timeout, this.timeout);

	@Override
	public Response invoke(final Request request, final Options... remoteOptions) throws Exception {
		Map<String, String> headers = this.handler.headers(request.code(), request.version(), request.context(),
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
		ResponseHandler resp = this.handler.post(path, headers, serializer.serialize(request.arguments()),
				serializer.defaultMimeType(), serializer.charset());
		return resp.response();
	}
}
