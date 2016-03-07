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
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		AsyncContext ac = request.startAsync();
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

		// set up ReadListener to read data for processing
		ServletInputStream input = request.getInputStream();
		ReadListener readListener = new ReadListenerImpl(input, response, ac);
		input.setReadListener(readListener);
		super.doPost(request, response);
	}

	class ReadListenerImpl implements ReadListener {
		private ServletInputStream input = null;
		private HttpServletResponse res = null;
		private AsyncContext ac = null;
		private Queue<String> queue = new LinkedBlockingQueue<String>();

		ReadListenerImpl(ServletInputStream in, HttpServletResponse r, AsyncContext c) {
			input = in;
			res = r;
			ac = c;
		}

		public void onDataAvailable() throws IOException {
			StringBuilder sb = new StringBuilder();
			int len = -1;
			byte b[] = new byte[1024];
			while (input.isReady() && (len = input.read(b)) != -1) {
				String data = new String(b, 0, len);
				sb.append(data);
			}
			queue.add(sb.toString());
		}

		public void onAllDataRead() throws IOException {
			// now all data are read, set up a WriteListener to write
			ServletOutputStream output = res.getOutputStream();
			WriteListener writeListener = new WriteListenerImpl(output, queue, ac);
			output.setWriteListener(writeListener);
		}

		public void onError(final Throwable t) {
			ac.complete();
			logger.error("Http async read failure", t);
		}
	}

	class WriteListenerImpl implements WriteListener {
		private ServletOutputStream output = null;
		private Queue<String> queue = null;
		private AsyncContext ac = null;

		WriteListenerImpl(ServletOutputStream sos, Queue<String> q, AsyncContext c) {
			output = sos;
			queue = q;
			ac = c;
		}

		public void onWritePossible() throws IOException {
			// write while there is data and is ready to write
			while (queue.peek() != null && output.isReady()) {
				String data = queue.poll();
				output.print(data);
			}
			// complete the async process when there is no more data to write
			if (queue.peek() == null) {
				ac.complete();
			}
		}

		public void onError(final Throwable t) {
			ac.complete();
			logger.error("Http async write failure", t);
		}
	}
}