package net.butfly.bus.impl;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebServiceAsyncFullServlet extends WebServiceServlet {
	private static final long serialVersionUID = 5048648001222215248L;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final AsyncContext ac = request.startAsync();
		ac.addListener(new AsyncListener() {
			@Override
			public void onComplete(final AsyncEvent event) throws IOException {
				logger.trace("Http async handle complet.");
			}

			@Override
			public void onError(final AsyncEvent event) {
				logger.error("Http async handle failure", event.getThrowable());
			}

			@Override
			public void onStartAsync(final AsyncEvent event) {
				logger.trace("Http async handle start.");
			}

			@Override
			public void onTimeout(final AsyncEvent event) {
				logger.error("Http async handle timeout", event.getThrowable());
			}
		});

		// set up ReadListener to read data for processing
		final ServletInputStream input = request.getInputStream();
		final ReadListener readListener = new ReadListenerImpl(input, response, ac);
		input.setReadListener(readListener);
		super.doPost(request, response);
	}

	class ReadListenerImpl implements ReadListener {
		private ServletInputStream input = null;
		private HttpServletResponse res = null;
		private AsyncContext ac = null;
		private final Queue<String> queue = new LinkedBlockingQueue<String>();

		ReadListenerImpl(final ServletInputStream in, final HttpServletResponse r, final AsyncContext c) {
			input = in;
			res = r;
			ac = c;
		}

		@Override
		public void onDataAvailable() throws IOException {
			final StringBuilder sb = new StringBuilder();
			int len = -1;
			final byte b[] = new byte[1024];
			while (input.isReady() && (len = input.read(b)) != -1) {
				final String data = new String(b, 0, len);
				sb.append(data);
			}
			queue.add(sb.toString());
		}

		@Override
		public void onAllDataRead() throws IOException {
			// now all data are read, set up a WriteListener to write
			final ServletOutputStream output = res.getOutputStream();
			final WriteListener writeListener = new WriteListenerImpl(output, queue, ac);
			output.setWriteListener(writeListener);
		}

		@Override
		public void onError(final Throwable t) {
			ac.complete();
			logger.error("Http async read failure", t);
		}
	}

	class WriteListenerImpl implements WriteListener {
		private ServletOutputStream output = null;
		private Queue<String> queue = null;
		private AsyncContext ac = null;

		WriteListenerImpl(final ServletOutputStream sos, final Queue<String> q, final AsyncContext c) {
			output = sos;
			queue = q;
			ac = c;
		}

		@Override
		public void onWritePossible() throws IOException {
			// write while there is data and is ready to write
			while (queue.peek() != null && output.isReady()) {
				final String data = queue.poll();
				output.print(data);
			}
			// complete the async process when there is no more data to write
			if (queue.peek() == null) {
				ac.complete();
			}
		}

		@Override
		public void onError(final Throwable t) {
			ac.complete();
			logger.error("Http async write failure", t);
		}
	}
}