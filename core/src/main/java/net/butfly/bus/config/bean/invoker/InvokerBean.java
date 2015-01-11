package net.butfly.bus.config.bean.invoker;

import net.butfly.bus.Token;
import net.butfly.bus.config.bean.ConfigBean;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.policy.Routeable;

public class InvokerBean extends ConfigBean implements Routeable {
	private static final long serialVersionUID = 1333442276226287430L;
	private String id;
	private String[] tx;
	private Class<? extends Invoker<?>> type;
	private InvokerConfigBean config;
	private Token token;

	public InvokerBean(String id, Class<? extends Invoker<?>> type, String tx, InvokerConfigBean config, Token token) {
		this.id = id;
		this.type = type;
		this.config = config;
		if (null == tx) throw new RuntimeException("invalid tx, maybe remote invoker TODO.");
		// this.tx = null == tx ? InvokerFactory.getInvoker(this).getTXCodes() : tx.split(",");
		this.tx = tx.split(",");
		this.token = token;
	}

	public String id() {
		return id;
	}

	public Class<? extends Invoker<?>> type() {
		return type;
	}

	public InvokerConfigBean config() {
		return config;
	}

	public String[] supportedTXs() {
		return this.tx;
	}

	public Token getToken() {
		return token;
	}
}
