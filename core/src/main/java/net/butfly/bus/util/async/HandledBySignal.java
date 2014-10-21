package net.butfly.bus.util.async;

import net.butfly.bus.argument.AsyncRequest;

public abstract class HandledBySignal {
	protected AsyncRequest request;

	public HandledBySignal(AsyncRequest request) {
		this.request = request;
	}

	protected abstract void handle() throws Throwable;

	public boolean retry() {
		return this.request.retry();
	}
}
