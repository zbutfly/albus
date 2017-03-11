package net.butfly.bus.utils.http;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

abstract class HttpWrapper implements Serializable {
	private static final long serialVersionUID = -6310365294081051362L;
	protected Map<String, String[]> headers;
	protected List<javax.servlet.http.Cookie> cookies;
	protected byte[] body;
}
