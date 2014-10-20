package net.butfly.bus.invoker;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map.Entry;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.http.HTTPAsyncUtils;
import net.butfly.albacore.utils.http.HTTPUtils;
import net.butfly.albacore.utils.serialize.HTTPStreamingSupport;
import net.butfly.albacore.utils.serialize.Serializer;
import net.butfly.albacore.utils.serialize.SerializerFactorySupport;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.invoker.WebServiceInvokerConfig;
import net.butfly.bus.ext.AsyncRequest;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.io.AbstractSerializerFactory;

public class WebServiceInvoker extends AbstractRemoteInvoker<WebServiceInvokerConfig> implements
		Invoker<WebServiceInvokerConfig> {
	private static Logger logger = LoggerFactory.getLogger(WebServiceInvoker.class);
	private static int DEFAULT_TIMEOUT = 5000;
	private String path;
	private int timeout;

	private Serializer serializer;
	private CloseableHttpClient client;
	private CloseableHttpAsyncClient asyncClient;

	@Override
	public void initialize(WebServiceInvokerConfig config, Token token) {
		this.path = config.getPath();
		this.timeout = config.getTimeout() > 0 ? config.getTimeout() : DEFAULT_TIMEOUT;
		this.serializer = config.getSerializer();
		if (!(this.serializer instanceof HTTPStreamingSupport) || !((HTTPStreamingSupport) this.serializer).supportHTTPStream())
			throw new SystemException("", "Serializer should support HTTP streaming mode.");
		if (this.serializer instanceof SerializerFactorySupport)
			for (Class<? extends AbstractSerializerFactory> f : config.getTypeTranslators())
				try {
					((SerializerFactorySupport) this.serializer).addFactory(f.newInstance());
				} catch (Exception e) {
					logger.error("Serializer factory instance construction failure for class: " + f.getName(), e);
					logger.error("Invoker initialization continued but the factory is ignored.");
				}

		super.initialize(config, token);

		this.asyncClient = HTTPAsyncUtils.create();
		this.client = HTTPUtils.createFull(this.timeout, -1);
	}

	protected void continuousInvoke(AsyncRequest request) {}

	protected Response singleInvoke(Request request) {
		HttpPost postReq = new HttpPost(this.path);
		postReq.setHeader("X-BUS-TX", request.code());
		postReq.setHeader("X-BUS-Version", request.version());
		if (request.context() != null) for (Entry<String, String> ctx : request.context().entrySet())
			postReq.setHeader("X-BUS-" + ctx.getKey(), ctx.getValue());
		postReq.setHeader(HttpHeaders.CONTENT_TYPE, ((HTTPStreamingSupport) this.serializer).getOutputContentType());

		PipedOutputStream os = new PipedOutputStream();
		try {
			this.serializer.write(os, request.arguments());
			InputStreamEntity e = new InputStreamEntity(new PipedInputStream(os), -1, ContentType.APPLICATION_OCTET_STREAM);
			e.setChunked(true);
			postReq.setEntity(e);

			CloseableHttpResponse postResp = this.client.execute(postReq);
			try {
				Response r = this.serializer.read(postResp.getEntity().getContent(), Response.class);
				EntityUtils.consume(postResp.getEntity());
				return r;
			} finally {
				postResp.close();
			}
		} catch (IOException e) {
			throw new SystemException("", e);
		}
	}
}
