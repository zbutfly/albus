package net.butfly.bus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class Request implements Serializable {
	private static final long serialVersionUID = -3216119686409193334L;
	String id;
	String code;
	String version;
	Object[] arguments;
	Map<String, String> context;

	Request() {}

	Request(String id, String code, String version, Map<String, String> context, Object[] arguments) {
		this.id = id;
		this.code = code;
		this.version = version;
		this.arguments = null == arguments ? new Object[0] : arguments;
		this.context = null == context ? new HashMap<String, String>() : context;
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
