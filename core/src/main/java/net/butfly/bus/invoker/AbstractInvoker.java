package net.butfly.bus.invoker;

import net.butfly.albacore.utils.Keys;
import net.butfly.bus.Token;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.context.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInvoker<C extends InvokerConfigBean> implements Invoker<C> {
	protected static Logger logger = LoggerFactory.getLogger(Invoker.class);
	protected C config;
	private Token token;
	private String id;

	@Override
	public void initialize(C config, Token token) {
		this.config = config;
		this.token = token;
		this.id = Keys.defaults();
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
