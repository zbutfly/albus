package net.butfly.bus.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class Token implements Serializable {
	private static final long serialVersionUID = 372303536302553583L;
	private String username;
	private String password;
	private String key;

	public Token(String username, String password) {
		super();
		this.username = username;
		this.password = password;
		this.key = null;
	}

	public Token(String key) {
		super();
		this.username = null;
		this.password = null;
		this.key = key;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getKey() {
		return key;
	}

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		if (null != username) map.put(Context.Key.USERNAME.name(), username);
		if (null != password) map.put(Context.Key.PASSWORD.name(), password);
		if (null != key) map.put(Context.Key.TOKEN.name(), key);
		return map;
	}
}
