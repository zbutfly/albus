package net.butfly.bus.comet;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.bus.context.Context;

public class CometContext {
	static final String CONTEXT_COMET_MANAGER_KEY = "CometContext";
//	static final String SERVLET_REQUEST_KEY = "ServletRequest";
//	static final String SERVLET_RESPONSE_KEY = "ServletResponse";
	static final long DEFAULT_COMET_TIMEOUT = 300000;
	static final long DEFAULT_COMET_INTERVAL = 5000;

	private final Map<String, AsyncContext> asyncContexts;

	public CometContext(HttpServletRequest request, HttpServletResponse response) {
		this.asyncContexts = new ConcurrentHashMap<String, AsyncContext>();

		Context.sourceHost(request.getRemoteAddr());
//		Context.CURRENT.put(SERVLET_REQUEST_KEY, request);
//		Context.CURRENT.put(SERVLET_RESPONSE_KEY, response);
		response.setHeader("Access-Control-Allow-Origin", "*");
		final String id = UUID.randomUUID().toString();
		if (!request.isAsyncStarted()) {
			final AsyncContext ac = request.startAsync(request, response);

			ac.addListener(new CometListener(id, asyncContexts));
			asyncContexts.put(id, ac);
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.setCharacterEncoding("utf-8");
		AsyncContext ac = asyncContexts.get(request.getParameter("metadata.id"));
		if (ac != null && "close".equals(request.getParameter("metadata.type"))) ac.complete();
		return;
	}

	public static void sleep(int timeout) {
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {}
	}
}
