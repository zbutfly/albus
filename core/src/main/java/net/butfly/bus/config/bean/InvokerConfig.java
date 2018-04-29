package net.butfly.bus.config.bean;

import java.util.Map;

import net.butfly.albacore.utils.Keys;
import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.TXs;
import net.butfly.bus.context.Token;
import net.butfly.bus.invoker.AbstractLocalInvoker;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.policy.Routeable;

public class InvokerConfig extends Config implements Routeable {
	private static final long serialVersionUID = 1333442276226287430L;
	private String id;
	private String[] txs;
	private Map<String, String> params;
	private Invoker instance = null;

	public InvokerConfig(Class<? extends Invoker> invokeClass, Map<String, String> params, String tx, Token token) {
		this.id = Keys.key(String.class);
		this.params = params;

		if (null != tx) this.txs = tx.split(",");
		else if (AbstractLocalInvoker.class.isAssignableFrom(invokeClass)) this.txs = null;
		else throw new RuntimeException("invalid txs for remote invoker");
		this.instance = Reflections.construct(invokeClass);
		this.instance.initialize(this, token);
		if (!this.instance.lazy()) this.instance.initialize();
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public boolean isSupported(String tx) {
		if (this.txs != null) return TXs.matching(tx, this.txs) >= 0;
		if (this.txs == null && instance instanceof AbstractLocalInvoker) return this.instance.isSupported(tx);
		return false;

	}

	public String param(String name, String... defaults) {
		String v = params.get(name);
		if (null != v) return v;
		if (defaults != null && defaults.length > 0) for (String d : defaults)
			if (null != d) return d;
		return null;
	}

	public Invoker invoker() {
		return this.instance;
	}
}
