package net.butfly.bus.invoker;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.http.HttpClientUtils;
import net.butfly.albacore.utils.serialize.HessianSerializer;
import net.butfly.albacore.utils.serialize.Serializer;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.auth.Token;
import net.butfly.bus.config.invoker.WebServiceInvokerConfig;
import net.butfly.bus.ext.AsyncRequest;

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
	private static long DEFAULT_TIMEOUT = 5000;
	private String path;
	private long timeout;
	private AbstractSerializerFactory[] translators;
	// private List<Class<? extends AbstractSerializerFactory>> translators;

	private Serializer serializer;
	private CloseableHttpClient client;
	private CloseableHttpAsyncClient asyncClient;

	@Override
	public void initialize(WebServiceInvokerConfig config, Token token) {
		this.path = config.getPath();
		this.timeout = config.getTimeout() > 0 ? config.getTimeout() : DEFAULT_TIMEOUT;
		try {
			this.serializer = config.getSerializer().newInstance();
		} catch (Exception e) {
			this.serializer = new HessianSerializer();
		}
		this.translators = this.createSerializers(config.getTypeTranslators());

		super.initialize(config, token);

		if (this.continuousSupported()) {
			this.asyncClient = HttpClientUtils.createAsync();
		} else {
			this.client = HttpClientUtils.create();
		}

	}

	private AbstractSerializerFactory[] createSerializers(List<Class<? extends AbstractSerializerFactory>> translators) {
		List<AbstractSerializerFactory> list = new ArrayList<AbstractSerializerFactory>(translators.size());
		for (Class<? extends AbstractSerializerFactory> clazz : translators) {
			try {
				list.add(clazz.newInstance());
			} catch (Exception ex) {
				logger.error("Type translator for hessian [" + clazz.getName() + "] invalid, ignored.");
			}
		}
		return list.toArray(new AbstractSerializerFactory[list.size()]);
	}

	protected void continuousInvoke(AsyncRequest request) {}

	protected Response singleInvoke(Request request) {
		HttpPost httppost = new HttpPost(this.path);
		PipedOutputStream os = new PipedOutputStream();
		try {
			this.serializer.write(os, request.arguments());
			InputStreamEntity reqEntity = new InputStreamEntity(new PipedInputStream(os), -1,
					ContentType.APPLICATION_OCTET_STREAM);
			reqEntity.setChunked(true);
			httppost.setEntity(reqEntity);
			System.out.println("Executing request: " + httppost.getRequestLine());
			CloseableHttpResponse response = this.client.execute(httppost);
			try {
				System.out.println("----------------------------------------");
				System.out.println(response.getStatusLine());
				Response r = this.serializer.read(response.getEntity().getContent(), Response.class);
				EntityUtils.consume(response.getEntity());
				return r;
			} finally {
				response.close();
			}
		} catch (IOException e) {
			throw new SystemException("", e);
		}

	}
}
