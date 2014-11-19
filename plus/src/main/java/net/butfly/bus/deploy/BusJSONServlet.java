package net.butfly.bus.deploy;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.bus.Bus;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.ServerWrapper;
import net.butfly.bus.TX;
import net.butfly.bus.policy.Router;
import net.butfly.bus.policy.SimpleRouter;
import net.butfly.bus.util.TXUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class BusJSONServlet extends BusServlet {
	private static final long serialVersionUID = 4533571572446977813L;
	private static Logger logger = LoggerFactory.getLogger(BusJSONServlet.class);
	private ServerWrapper servers;
	private Router router;
	private Gson gson = new Gson();
	private JsonParser parser = new JsonParser();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			logger.trace("Bus starting...");
			servers = ServerWrapper.construct(this.getInitParameter("config-file"), this.getInitParameter("server-class"));
			try {
				router = (Router) Class.forName(this.getInitParameter("router-class")).newInstance();
			} catch (Throwable th) {
				router = new SimpleRouter();
			}
			logger.info("Bus started.");
		} catch (Throwable ex) {
			logger.error("Bus starting failed: ", ex);
			throw new ServletException(ex);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Response resp = this.invoke((HttpServletRequest) request);
		response.setStatus(200);
		response.setContentType("application/json; charset=utf-8");
		response.setHeader("ETag", resp.id());
		PrintWriter out = response.getWriter();
		out.print(gson.toJson(resp));
		out.flush();
	}

	private Map<String, String> context(HttpServletRequest request) {
		String json = request.getHeader("X-BUS-Context");
		Map<String, String> map = gson.fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
		return map;
	}

	private Object[] arguments(HttpServletRequest request, Class<?>[] parameterTypes) throws ServletException {
		try {
			JsonElement ele = parser.parse(request.getReader());
			if (ele.isJsonNull()) return null;
			if (ele.isJsonObject() || ele.isJsonPrimitive()) {
				if (parameterTypes.length != 1) throw new IllegalArgumentException();
				return new Object[] { gson.fromJson(ele, parameterTypes[0]) };
			}
			if (ele.isJsonArray()) {
				JsonArray arr = ele.getAsJsonArray();
				if (arr.size() != parameterTypes.length) throw new IllegalArgumentException();
				Object[] args = new Object[parameterTypes.length];
				for (int i = 0; i < parameterTypes.length; i++)
					args[i] = gson.fromJson(arr.get(i), parameterTypes[i]);
				return args;
			}
			throw new IllegalArgumentException();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private Response invoke(HttpServletRequest request) throws ServletException {
		TX tx = this.tx((HttpServletRequest) request);
		Bus server = this.router.route(tx.value(), servers.servers());
		Object[] arguments = this.arguments((HttpServletRequest) request, server.getParameterTypes(tx.value(), tx.version()));
		Map<String, String> context = this.context((HttpServletRequest) request);
		return server.invoke(new Request(tx, context, arguments));
	}

	private TX tx(HttpServletRequest request) throws ServletException {
		String[] reses = request.getPathInfo().substring(1).split("/");
		String code, version = null;
		switch (reses.length) {
		case 0: // anything in header.
			code = request.getHeader("X-BUS-TX");
			version = request.getHeader("X-BUS-TX-Version");
			break;
		case 2:
			version = reses[1];
		case 1:
			code = reses[0];
			break;
		default:
			throw new ServletException();
		}
		return TXUtils.TXImpl(code, version);
	}
}
