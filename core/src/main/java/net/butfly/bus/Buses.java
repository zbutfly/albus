package net.butfly.bus;

import java.util.HashMap;
import java.util.Map;

import net.butfly.albacore.utils.Keys;
import net.butfly.bus.context.Context.Key;

public interface Buses {

	static Response response(Request request) {
		Response r = new Response();
		r.id = Keys.key(String.class);
		r.requestId = request.id;
		return r;
	}

	static Request request(String code) {
		return request(code, TX.ALL_VERSION, new HashMap<String, String>(), new Object[0]);
	}

	static Request request(String code, Object... arguments) {
		return request(code, TX.ALL_VERSION, new HashMap<String, String>(), arguments);
	}

	static Request request(String code, Map<String, String> context) {
		return request(code, TX.ALL_VERSION, context, new Object[0]);
	}

	static Request request(String code, Map<String, String> context, Object... arguments) {
		return request(code, TX.ALL_VERSION, context, arguments);
	}

	static Request request(String code, String version) {
		return request(code, version, new HashMap<String, String>(), new Object[0]);
	}

	static Request request(String code, String version, Object... arguments) {
		return request(code, version, new HashMap<String, String>(), arguments);
	}

	static Request request(String code, String version, Map<String, String> context) {
		return request(code, version, context, new Object[0]);
	}

	static Request request(TX tx) {
		return request(tx.value(), tx.version());
	}

	static Request request(TX tx, Map<String, String> context) {
		return request(tx.value(), tx.version(), context);
	}

	static Request request(TX tx, Object... arguments) {
		return request(tx.value(), tx.version(), arguments);
	}

	static Request request(TX tx, Map<String, String> context, Object... arguments) {
		return request(tx.value(), tx.version(), context, arguments);
	}

	static Request request(String code, String version, Map<String, String> context, Object... arguments) {
		Request r = new Request();
		r.id = Keys.key(String.class);
		r.code = code;
		r.version = version;
		r.arguments = null == arguments ? new Object[0] : arguments;
		r.context = null == context ? new HashMap<String, String>() : context;
		r.context.put(Key.RequestID.name(), r.id);
		return r;
	}
}
