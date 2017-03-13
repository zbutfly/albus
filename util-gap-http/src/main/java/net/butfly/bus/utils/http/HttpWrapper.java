package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.butfly.albacore.utils.IOs;

abstract class HttpWrapper<R extends HttpWrapper<R>> implements Serializable {
	private static final long serialVersionUID = -6310365294081051362L;
	protected Map<String, String[]> headers;
	protected List<javax.servlet.http.Cookie> cookies;
	protected byte[] body;

	public HttpWrapper() {
		headers = new ConcurrentHashMap<>();
		cookies = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public R load(InputStream in) throws IOException {
		for (int i = 0; i < IOs.readInt(in); i++)
			headers.put(new String(IOs.readBytes(in)), IOs.readBytes(in, IOs.readInt(in), b -> new String(b)).toArray(new String[0]));
		for (int i = 0; i < IOs.readInt(in); i++)
			cookies.add(IOs.readObj(in));
		body = IOs.readBytes(in);
		return (R) this;
	}

	@SuppressWarnings("unchecked")
	public R save(OutputStream out) throws IOException {
		IOs.writeInt(out, headers.size());
		for (Entry<String, String[]> h : headers.entrySet()) {
			IOs.writeBytes(out, h.getKey().getBytes());
			IOs.writeInt(out, h.getValue().length);
			IOs.writeBytes(out, Arrays.stream(h.getValue()).map(v -> v.getBytes()).collect(Collectors.toList()).toArray(new byte[0][]));
		}
		IOs.writeInt(out, cookies.size());
		for (javax.servlet.http.Cookie c : cookies)
			IOs.writeObj(out, c);
		IOs.writeBytes(out, body);
		return (R) this;
	}
}
