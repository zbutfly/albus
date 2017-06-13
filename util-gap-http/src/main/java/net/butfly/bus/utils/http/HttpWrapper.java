package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.butfly.albacore.utils.IOs;

abstract class HttpWrapper<R extends HttpWrapper<R>> implements Serializable {
	private static final long serialVersionUID = -6310365294081051362L;
	protected Map<String, Collection<String>> headers;
	protected List<javax.servlet.http.Cookie> cookies;
	protected byte[] body;

	public HttpWrapper() {
		headers = new ConcurrentHashMap<>();
		cookies = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public R load(InputStream in) {
		try {
			int c = IOs.readInt(in);
			for (int i = 0; i < c; i++) {
				String key = new String(IOs.readBytes(in));
				List<String> values = IOs.readBytes(in, b -> new String(b));
				headers.put(key, values);
			}
			c = IOs.readInt(in);
			for (int i = 0; i < c; i++)
				cookies.add(IOs.readObj(in));
			body = IOs.readBytes(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	public R save(OutputStream out) {
		try {
			IOs.writeInt(out, headers.size());
			for (Entry<String, Collection<String>> h : headers.entrySet()) {
				IOs.writeBytes(out, h.getKey().getBytes());
				byte[][] value = h.getValue().parallelStream().map(v -> v.getBytes()).collect(Collectors.toList()).toArray(new byte[0][]);
				IOs.writeBytes(out, value);
			}
			IOs.writeInt(out, cookies.size());
			for (javax.servlet.http.Cookie c : cookies)
				IOs.writeObj(out, c);
			IOs.writeBytes(out, body);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return (R) this;
	}
}
