package net.butfly.bus.invoker;

import net.butfly.albacore.utils.logger.Logger;

import net.butfly.albacore.utils.Keys;
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.context.Context;
import net.butfly.bus.context.Token;

public abstract class AbstractInvoker implements Invoker {
	protected static Logger logger = Logger.getLogger(Invoker.class);
	protected InvokerConfig config;
	private Token token;
	private String id;

	@Override
	public void initialize(InvokerConfig config, Token token) {
		this.config = config;
		this.token = token;
		this.id = Keys.key(String.class);
	}

	@Override
	public void initialize() {
		this.config = null;
	}

	public boolean initialized() {
		return this.config == null;
	}

	public void setToken(Token token) {
		this.token = token;
	}

	@Override
	public final Token token() {
		Token t = Context.token();
		return null == t ? this.token : t;
	}

	@Override
	public String id() {
		return this.id;
	}
}
