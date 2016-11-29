package net.butfly.bus.impl;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.albacore.utils.Exceptions;

public class WebServiceAsyncWriteServlet extends WebServiceServlet {
	private static final long serialVersionUID = 5048648001222215248L;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	@Override
	protected void doPost(HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final ServiceContext context = this.prepare(request, response);
		final Queue<byte[]> queue = this.parareAsync(request, response);
		try {
			cluster.invoke(context.invoking, resp -> {
				try {
					byte[] data = context.handler.response(resp, response, context.invoking.supportClass, context.respContentType
							.getCharset());
					queue.add(data);
					response.getOutputStream().flush();
					// response.flushBuffer();
				} catch (IOException ex) {
					logger.error("Response writing I/O failure", ex);
				}
			});
		} catch (Exception ex) {
			throw Exceptions.wrap(ex, ServletException.class);
		}

	}

	private Queue<byte[]> parareAsync(HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final AsyncContext ac = request.startAsync();
		ac.addListener(new AsyncListener() {
			public void onComplete(AsyncEvent event) throws IOException {
				logger.trace("Http async handle complet.");;
			}

			public void onError(AsyncEvent event) {
				logger.error("Http async handle failure", event.getThrowable());
			}

			public void onStartAsync(AsyncEvent event) {
				logger.trace("Http async handle start.");;
			}

			public void onTimeout(AsyncEvent event) {
				logger.error("Http async handle timeout", event.getThrowable());
			}
		});
		final ServletOutputStream output = response.getOutputStream();
		final Queue<byte[]> queue = new LinkedBlockingQueue<byte[]>();
		output.setWriteListener(new WriteListener() {
			public void onWritePossible() throws IOException {
				while (queue.peek() != null && output.isReady())
					output.write(queue.poll());
				if (queue.peek() == null) ac.complete();
			}

			public void onError(final Throwable t) {
				ac.complete();
				logger.error("Http async write failure", t);
			}
		});
		return queue;
	}
}