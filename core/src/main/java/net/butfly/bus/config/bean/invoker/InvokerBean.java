package net.butfly.bus.config.bean.invoker;

import net.butfly.bus.Token;
import net.butfly.bus.config.bean.ConfigBean;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.policy.Routeable;
import net.butfly.bus.utils.TXUtils;

public class InvokerBean extends ConfigBean implements Routeable {
	private static final long serialVersionUID = 1333442276226287430L;
	private String id;
	private String[] txs;
	private Class<? extends Invoker<?>> type;
	private InvokerConfigBean config;
	private Token token;

	public InvokerBean(String id, Class<? extends Invoker<?>> type, String tx, InvokerConfigBean config, Token token) {
		this.id = id;
		this.type = type;
		this.config = config;
		if (null == tx) throw new RuntimeException("invalid txs, maybe remote invoker TODO.");
		// this.tx = null == txs ? InvokerFactory.getInvoker(this).getTXCodes() : txs.split(",");
		this.txs = tx.split(",");
		this.token = token;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public boolean isSupported(String tx) {
		return TXUtils.matching(tx, this.txs) >= 0;
	}

	public Class<? extends Invoker<?>> type() {
		return type;
	}

	public InvokerConfigBean config() {
		return config;
	}

	public Token getToken() {
		return token;
	}
}
