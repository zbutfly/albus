package net.butfly.bus.utils.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.albacore.utils.async.Options;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.context.BusHttpHeaders;
import net.butfly.bus.invoker.WebServiceInvoker.HandlerResponse;
import net.butfly.bus.serialize.Serializer;
import net.butfly.bus.utils.TXUtils;

import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;

public abstract class HttpHandler {
	protected static Logger logger = LoggerFactory.getLogger(HttpHandler.class);
	protected int connTimeout;
	protected int readTimeout;

	public HttpHandler(int connTimeout, int readTimeout) {
		this.connTimeout = connTimeout >= 0 ? connTimeout : 0;
		this.readTimeout = readTimeout >= 0 ? readTimeout : 0;
	}

	public abstract HandlerResponse post(String url, Map<String, String> headers, byte[] data, String mimeType,
			Charset charset, boolean streaming) throws IOException;

	protected static void logRequest(String url, Map<String, String> headers, byte[] sent, Charset charset, boolean streaming) {
		if (logger.isTraceEnabled()) {
			logger.trace("HTTP Request SEND ==> STREAM: " + (streaming ? "YES" : "NO") + " ==> " + url);
			logger.trace("HTTP Request SEND ==> HEADER: " + headers.toString());
			logger.trace("HTTP Request SEND ==> CONTENT[" + sent.length + "]: " + new String(sent, charset));
		}
	}

	public static Map<String, String> headers(final HttpServletRequest request) {
		Map<String, String> busHeaders = new HashMap<String, String>();
		Enumeration<String> en = request.getHeaderNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if (name != null && name.startsWith(BusHttpHeaders.HEADER_PREFIX))
			// XXX: multiple values header?
				busHeaders.put(name, request.getHeader(name));
		}
		if (logger.isTraceEnabled()) logger.trace("HTTP Request RECV <== HEADER: " + busHeaders);
		return busHeaders;
	}

	public static Map<String, String> headers(final HttpServletResponse response) {
		Map<String, String> busHeaders = new HashMap<String, String>();
		for (String name : response.getHeaderNames())
			if (name != null && name.startsWith(BusHttpHeaders.HEADER_PREFIX)) busHeaders.put(name, response.getHeader(name));
		return busHeaders;
	}

	// used by WebServiceServlet
	public static Map<String, String> context(Map<String, String> busHeaders) {
		Map<String, String> context = new HashMap<String, String>();
		for (String name : busHeaders.keySet()) {
			if (name.startsWith(BusHttpHeaders.HEADER_CONTEXT_PREFIX))
				context.put(name.substring(BusHttpHeaders.HEADER_CONTEXT_PREFIX.length()), busHeaders.get(name));
		}
		return context;
	}

	public static TX tx(String pathInfo, Map<String, String> busHeaders) {
		String[] reses = null != pathInfo && pathInfo.length() > 0 ? pathInfo.substring(1).split("/") : new String[0];
		String code, version = null;
		switch (reses.length) {
		case 0: // anything in header.
			code = busHeaders.get(BusHttpHeaders.HEADER_TX_CODE);
			version = busHeaders.get(BusHttpHeaders.HEADER_TX_VERSION);
			break;
		case 2:
			version = reses[1];
		case 1:
			code = reses[0];
			break;
		default:
			throw new RuntimeException("Invalid path: " + pathInfo);
		}
		return TXUtils.TXImpl(code, version);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object[] parameters(byte[] recv, Serializer serializer, Class<?>[] parameterClasses, Charset charset) {
		if (logger.isTraceEnabled())
			logger.trace("HTTP Request RECV <== CONTENT[" + recv.length + "]: " + new String(recv, charset));
		Object r = serializer.deserialize(recv, parameterClasses);
		if (r == null) return new Object[0];
		else if (r.getClass().isArray()) return (Object[]) r;
		else if (Collection.class.isAssignableFrom(r.getClass())) {
			Collection c = Collection.class.cast(r);
			return c.toArray(new Object[c.size()]);
		} else if (Iterable.class.isAssignableFrom(r.getClass())) {
			Iterable i = Iterable.class.cast(r);
			List l = new ArrayList();
			for (Iterator it = i.iterator(); it.hasNext();)
				l.add(it.next());
			return l.toArray(new Object[l.size()]);
		} else if (Enumeration.class.isAssignableFrom(r.getClass())) {
			List l = new ArrayList();
			for (Enumeration e = Enumeration.class.cast(r); e.hasMoreElements();)
				l.add(e.nextElement());
			return l.toArray(new Object[l.size()]);
		} else return new Object[] { r };
	}

	public static byte[] response(final Response resp, final HttpServletResponse response, final Serializer serializer,
			boolean supportClass, Charset charset) {
		response.setHeader(HttpHeaders.ETAG, resp.id());
		response.setHeader(BusHttpHeaders.HEADER_REQUEST_ID, resp.requestId());
		if (resp.context() != null) for (Entry<String, String> ctx : resp.context().entrySet())
			response.setHeader(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());

		boolean error = resp.error() != null;
		if (supportClass) response.setHeader(BusHttpHeaders.HEADER_CLASS_SUPPORT, Boolean.toString(true));
		byte[] sent;
		if (error) {
			response.setHeader(BusHttpHeaders.HEADER_ERROR, Boolean.toString(true));
			sent = serializer.serialize(resp.error());
		} else {
			if (supportClass && resp.result() != null)
				response.setHeader(BusHttpHeaders.HEADER_CLASS, TypeToken.of(resp.result().getClass()).toString());
			sent = serializer.serialize(resp.result());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("HTTP Response SEND ==> HEADER: " + headers(response));
			logger.trace("HTTP Response SEND ==> CONTENT[" + sent.length + "]: " + new String(sent, charset));
		}
		return sent;
	}

	// for client
	public static Map<String, String> headers(Serializer serializer, String tx, String version, Map<String, String> context,
			Options... options) throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHttpHeaders.HEADER_TX_CODE, tx);
		headers.put(BusHttpHeaders.HEADER_TX_VERSION, version);
		headers.put(BusHttpHeaders.HEADER_CLASS_SUPPORT, Boolean.toString(!serializer.supportClass()));
		if (context != null) for (Entry<String, String> ctx : context.entrySet())
			headers.put(BusHttpHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
		if (null != options && options.length > 0) headers.put(BusHttpHeaders.HEADER_OPTIONS, serializer.asString(options));

		return headers;
	}
}
