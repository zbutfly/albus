package net.butfly.bus.utils.http;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.google.common.reflect.TypeToken;

import net.butfly.albacore.exception.NotImplementedException;
import net.butfly.albacore.serder.TextSerder;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Opts;
import net.butfly.albacore.utils.async.Task;
import net.butfly.bus.Error;
import net.butfly.bus.Response;
import net.butfly.bus.TX;
import net.butfly.bus.TXs;
import net.butfly.bus.filter.LoggerFilter;

public class HttpHandler {
	protected static Logger logger = LoggerFactory.getLogger(HttpHandler.class);
	protected TextSerder serializer;

	public HttpHandler(TextSerder serializer) {
		this.serializer = serializer;
	}

	public ResponseHandler post(final BusHttpRequest httpRequest) throws IOException {
		throw new NotImplementedException();
	}

	public Future<Void> post(final BusHttpRequest httpRequest, final Task.Callback<Map<String, String>> contextCallback,
			final Task.Callback<Response> responseCallback, final Task.ExceptionHandler<ResponseHandler> exception) throws IOException {
		ResponseHandler resp = this.post(httpRequest);
		contextCallback.callback(resp.context());
		responseCallback.callback(resp.response());
		return null;
	}

	protected static void logRequest(String url, Map<String, String> headers, byte[] sent, Charset charset) {
		if (logger.isTraceEnabled()) {
			logger.trace("HTTP Request SEND ==> " + url);
			logger.trace("HTTP Request SEND ==> HEADER: " + headers.toString());
			logger.trace("HTTP Request SEND ==> CONTENT[" + sent.length + "]: " + new String(sent, charset));
		}
	}

	public final Map<String, String> headers(final HttpServletRequest request) {
		Map<String, String> busHeaders = new HashMap<String, String>();
		Enumeration<String> en = request.getHeaderNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if (name != null && name.startsWith(BusHeaders.HEADER_PREFIX))
				// XXX: multiple values header?
				busHeaders.put(name, request.getHeader(name));
		}
		if (logger.isTraceEnabled()) logger.trace("HTTP Request RECV <== HEADER: " + busHeaders);
		return busHeaders;
	}

	public Map<String, String> headers(final HttpServletResponse response) {
		Map<String, String> busHeaders = new HashMap<String, String>();
		for (String name : response.getHeaderNames())
			if (name != null && name.startsWith(BusHeaders.HEADER_PREFIX)) busHeaders.put(name, response.getHeader(name));
		return busHeaders;
	}

	// used by WebServiceServlet
	public Map<String, String> context(Map<String, String> busHeaders) {
		Map<String, String> context = new HashMap<String, String>();
		for (String name : busHeaders.keySet()) {
			if (name.startsWith(BusHeaders.HEADER_CONTEXT_PREFIX)) context.put(name.substring(BusHeaders.HEADER_CONTEXT_PREFIX.length()),
					busHeaders.get(name));
		}
		return context;
	}

	public TX tx(String pathInfo, Map<String, String> busHeaders) {
		String[] reses = null != pathInfo && pathInfo.length() > 0 ? pathInfo.substring(1).split("/") : new String[0];
		String code, version = null;
		switch (reses.length) {
		case 0: // anything in header.
			code = busHeaders.get(BusHeaders.HEADER_TX_CODE);
			version = busHeaders.get(BusHeaders.HEADER_TX_VERSION);
			break;
		case 2:
			version = reses[1];
		case 1:
			code = reses[0];
			break;
		default:
			throw new RuntimeException("Invalid path: " + pathInfo);
		}
		return TXs.impl(code, version);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object[] parameters(byte[] recv, Charset charset, Class<? extends Serializable>... parameterClasses) {
		if (logger.isTraceEnabled()) logger.trace("HTTP Request RECV <== CONTENT[" + recv.length + "]: " + new String(recv, charset));
		Object r = serializer.deserialize(new String(recv, charset), parameterClasses);
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

	public byte[] response(final Response resp, final HttpServletResponse response, boolean supportClass, Charset charset) {
		response.setHeader(HttpHeaders.ETAG, resp.id());
		response.setHeader(BusHeaders.HEADER_REQUEST_ID, resp.requestId());
		if (resp.context() != null) for (Entry<String, String> ctx : resp.context().entrySet())
			response.setHeader(BusHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());

		boolean error = resp.error() != null;
		if (supportClass) response.setHeader(BusHeaders.HEADER_CLASS_SUPPORT, Boolean.toString(true));
		byte[] sent;
		if (error) {
			if (!supportClass) response.setHeader(BusHeaders.HEADER_CLASS, TypeToken.of(Error.class).toString());
			response.setHeader(BusHeaders.HEADER_ERROR, Boolean.toString(true));
			sent = serializer.toBytes(resp.error());
		} else {
			if (!supportClass && resp.result() != null) response.setHeader(BusHeaders.HEADER_CLASS, TypeToken.of(resp.result().getClass())
					.toString());
			sent = serializer.toBytes((Serializable) resp.result());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("HTTP Response SEND ==> HEADER: " + headers(response));
			logger.trace("HTTP Response SEND ==> CONTENT[" + sent.length + "]: " + LoggerFilter.shrink(new String(sent, charset)));
		}
		return sent;
	}

	private Opts opts = new MoreOpts();

	// for client
	public Map<String, String> headers(String tx, String version, Map<String, String> context, boolean supportClass, Options... options)
			throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(BusHeaders.HEADER_TX_CODE, tx);
		headers.put(BusHeaders.HEADER_TX_VERSION, version);
		headers.put(BusHeaders.HEADER_CLASS_SUPPORT, Boolean.toString(supportClass));
		if (context != null) for (Entry<String, String> ctx : context.entrySet())
			headers.put(BusHeaders.HEADER_CONTEXT_PREFIX + ctx.getKey(), ctx.getValue());
		if (null != options && options.length > 0) headers.put(BusHeaders.HEADER_OPTIONS, this.opts.format(options));
		return headers;
	}

	public String source(HttpServletRequest request) {
		String ip = request.getHeader(HttpHeaders.X_FORWARDED_FOR);
		if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip.trim())) return ip.split(",")[0].trim();
		ip = request.getHeader("Proxy-Client-IP");
		if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip.trim())) return ip;
		ip = request.getHeader("WL-Proxy-Client-IP");
		if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip.trim())) return ip;
		return request.getRemoteAddr();
	}
}
