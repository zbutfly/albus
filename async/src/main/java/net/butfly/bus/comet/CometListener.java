package net.butfly.bus.comet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import net.butfly.albacore.utils.logger.Logger;

public final class CometListener implements AsyncListener {
	private Logger logger = Logger.getLogger(CometListener.class);
	private String asyncId;
	private Map<String, AsyncContext> asyncContexts;

	public CometListener(String asyncId, Map<String, AsyncContext> asyncContext) {
		this.asyncId = asyncId;
		this.asyncContexts = asyncContext;
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		logger.trace("Comet [" + asyncId + "] completed.");
		asyncContexts.remove(asyncId);
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		logger.trace("Comet [" + asyncId + "] completed.");
		asyncContexts.remove(asyncId);
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		logger.trace("Comet [" + asyncId + "] completed.");
		asyncContexts.remove(asyncId);
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {}
}
