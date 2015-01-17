package net.butfly.bus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.KeyUtils;
import net.butfly.bus.context.Context.Key;

public class Request implements Serializable {
	private static final long serialVersionUID = -3216119686409193334L;
	protected String id;
	private String code;
	private String version;
	private Object[] arguments;
	private Map<String, String> context;

	protected Request() {}

	public Request(String code) {
		this(code, TX.ALL_VERSION, new HashMap<String, String>(), new Object[0]);
	}

	public Request(String code, Object... arguments) {
		this(code, TX.ALL_VERSION, new HashMap<String, String>(), arguments);
	}

	public Request(String code, Map<String, String> context) {
		this(code, TX.ALL_VERSION, context, new Object[0]);
	}

	public Request(String code, Map<String, String> context, Object... arguments) {
		this(code, TX.ALL_VERSION, context, arguments);
	}

	public Request(String code, String version) {
		this(code, version, new HashMap<String, String>(), new Object[0]);
	}

	public Request(String code, String version, Object... arguments) {
		this(code, version, new HashMap<String, String>(), arguments);
	}

	public Request(String code, String version, Map<String, String> context) {
		this(code, version, context, new Object[0]);
	}

	public Request(TX tx) {
		this(tx.value(), tx.version());
	}

	public Request(TX tx, Map<String, String> context) {
		this(tx.value(), tx.version(), context);
	}

	public Request(TX tx, Object... arguments) {
		this(tx.value(), tx.version(), arguments);
	}

	public Request(TX tx, Map<String, String> context, Object... arguments) {
		this(tx.value(), tx.version(), context, arguments);
	}

	protected Request(String id, String code, String version, Map<String, String> context, Object[] arguments) {
		this.id = id;
		this.code = code;
		this.version = version;
		this.arguments = null == arguments ? new Object[0] : arguments;
		this.context = null == context ? new HashMap<String, String>() : context;
	}

	public Request(String code, String version, Map<String, String> context, Object... arguments) {
		this.id = KeyUtils.objectId();
		this.code = code;
		this.version = version;
		this.arguments = null == arguments ? new Object[0] : arguments;
		this.context = null == context ? new HashMap<String, String>() : context;
		this.context.put(Key.RequestID.name(), this.id);
	}

	public String id() {
		return id;
	}

	public String code() {
		return code;
	}

	public String version() {
		return version;
	}

	public String context(String key) {
		return this.context.get(key);
	}

	public String context(String key, String value) {
		String val = this.context.get(key);
		this.context.put(key, value);
		return val;
	}

	public void context(Map<String, String> context) {
		this.context.putAll(context);
	}

	public Map<String, String> context() {
		return this.context;
	}

	@Deprecated
	public void argument(int index, Object value) {
		if (index >= this.arguments.length) {
			Object[] args = new Object[index - 1];
			for (int i = 0; i < index - 1; i++)
				args[i] = i < this.arguments.length ? this.arguments[i] : null;
			this.arguments = args;
		}
		this.arguments[index] = value;
	}

	@Deprecated
	public void arguments(Object... values) {
		this.arguments = values;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\tdetail of current invkoing: ").append("\n\t\ttx context: ").append(this.context.toString())
				.append("\n\t\ttx code: ").append(this.code).append("\n\t\ttx version: ")
				.append(null == this.version ? "NULL" : this.version).append("\n\t\tparametres: ");
		if (this.arguments != null) {
			for (Object to : this.arguments) {
				if (null != to) {
					sb.append("\n\t\t\t").append(to);
				}
			}
		} else {
			sb.append("[empty]");
		}
		return sb.toString();
	}

	public Object[] arguments() {
		return this.arguments;
	}

	public Request token(Token token) {
		if (null != token) this.context.putAll(token.toMap());
		return this;
	}
}
