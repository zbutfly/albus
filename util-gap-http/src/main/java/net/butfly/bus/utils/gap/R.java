package net.butfly.bus.utils.gap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

abstract class R implements Serializable {
	private static final long serialVersionUID = -6310365294081051362L;
	protected Map<String, String[]> headers;
	protected List<javax.servlet.http.Cookie> cookies;
	protected byte[] body;
}
