package net.butfly.bus.utils.http;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.butfly.albacore.utils.Exceptions;
import net.butfly.albacore.utils.async.Task.Callback;
import net.butfly.albacore.utils.async.Task.ExceptionHandler;
import net.butfly.bus.serialize.Serializer;

import org.apache.http.entity.ContentType;

import com.google.common.net.HttpHeaders;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;

public class HttpNingHandler extends HttpHandler {
	private static final AsyncHttpClient client = new AsyncHttpClient(new NettyAsyncHttpProvider(
			new AsyncHttpClientConfig.Builder().setRequestTimeout(Integer.MAX_VALUE).setReadTimeout(Integer.MAX_VALUE).build()));

	public HttpNingHandler(Serializer serializer) {
		super(serializer);
//		this.client = Instances.fetch(new Task.Callable<AsyncHttpClient>() {
//			@Override
//			public AsyncHttpClient create() {
//				return new AsyncHttpClient(new NettyAsyncHttpProvider(new AsyncHttpClientConfig.Builder()
//						.setRequestTimeout(Integer.MAX_VALUE).setReadTimeout(Integer.MAX_VALUE).build()));
//			}
//		});
	}

	@Override
	public ResponseHandler post(BusHttpRequest httpRequest) throws IOException {
		BoundRequestBuilder req = this.prepare(httpRequest);
		req.setRequestTimeout(httpRequest.timeout);
		Response resp;
		try {
			resp = req.execute().get();
		} catch (InterruptedException e) {
			throw new IOException("Async Http interrupted", e);
		} catch (ExecutionException e) {
			throw new IOException("Async Http failure", e.getCause());
		}
		return this.process(resp);
	}

	@Override
	public Future<Void> post(BusHttpRequest httpRequest, final Callback<Map<String, String>> contextCallback,
			final Callback<net.butfly.bus.Response> responseCallback, final ExceptionHandler<ResponseHandler> exception)
			throws IOException {
		BoundRequestBuilder req = this.prepare(httpRequest);
		return req.execute(new AsyncHandler<Void>() {
			@Override
			public void onThrowable(Throwable t) {
				logger.error("Http failure", t);
				Exception ex = t instanceof Exception ? (Exception) t : Exceptions.wrap(t);
				try {
					exception.handle(ex);
				} catch (Exception e) {
					throw Exceptions.wrap(e);
				}
			}

			@Override
			public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
				// TODO: async
				responseCallback.callback(new ResponseHandler(serializer, null, bodyPart.getBodyPartBytes()).response());
				return STATE.CONTINUE;
			}

			@Override
			public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
				int statusCode = responseStatus.getStatusCode();
				return statusCode >= 400 ? STATE.ABORT : STATE.CONTINUE;
			}

			@Override
			public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
				contextCallback.callback(new ResponseHandler(serializer, headers.getHeaders(), null).context());
				return null;
			}

			@Override
			public Void onCompleted() throws Exception {
				return null;
			}
		});
	}

	private BoundRequestBuilder prepare(BusHttpRequest httpRequest) {
		httpRequest.logRequest(logger);

		BoundRequestBuilder req = client.preparePost(httpRequest.url);
		for (String name : httpRequest.headers.keySet())
			req.setHeader(name, httpRequest.headers.get(name));
		req.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.create(httpRequest.mimeType, httpRequest.charset).toString());
		req.setHeader(HttpHeaders.ACCEPT_ENCODING, "deflate");
		req.setBody(httpRequest.data);
		return req;
	}

	private ResponseHandler process(Response resp) throws IOException {
		int statusCode = resp.getStatusCode();
		if (statusCode != 200) throw new IOException("Async Http resposne status code: " + statusCode);

		Map<String, List<String>> recvHeaders = resp.getHeaders();
		byte[] recv = resp.getResponseBodyAsBytes();
		return new ResponseHandler(serializer, recvHeaders, recv);
	}
}
