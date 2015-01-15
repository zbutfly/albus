package net.butfly.bus.config.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.KeyUtils;
import net.butfly.albacore.utils.XMLUtils;
import net.butfly.bus.Token;
import net.butfly.bus.config.Config;
import net.butfly.bus.config.ConfigParser;
import net.butfly.bus.config.bean.FilterBean;
import net.butfly.bus.config.bean.RouterBean;
import net.butfly.bus.config.bean.invoker.InvokerBean;
import net.butfly.bus.config.bean.invoker.InvokerConfigBean;
import net.butfly.bus.filter.Filter;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.invoker.InvokerFactory;
import net.butfly.bus.policy.Router;
import net.butfly.bus.utils.Constants;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XMLConfigParser extends ConfigParser {
	protected Document document;
	protected Element root;

	@Override
	public Config parse() {
		Config config = new Config();
		config.setFilterList(this.parseFilters(this.elements("filter")));
		config.setInvokers(this.parseInvokers());
		config.setRouter(this.parseRouter());
		return config;
	}

	public XMLConfigParser(InputStream source) {
		super();
		if (null == source) throw new SystemException(Constants.UserError.CONFIG_ERROR, "StandardBus configurations invalid.");
		try {
			this.document = new SAXReader().read(source);
		} catch (DocumentException e) {
			throw new SystemException(Constants.UserError.CONFIG_ERROR, "StandardBus configurations invalid.", e);
		}
		this.root = this.document.getRootElement();
	}

	protected InvokerBean[] parseInvokers() {
		List<Element> elements = this.elements("invoker");
		List<InvokerBean> beans = new ArrayList<InvokerBean>();
		if (elements == null || elements.size() == 0)
			throw new SystemException(Constants.UserError.CONFIG_ERROR, "No invoker found, bus could not work.");
		for (Element element : elements) {
			InvokerBean ivk = this.parseInvoker(element);
			if (null != ivk) beans.add(ivk);
		}
		return beans.toArray(new InvokerBean[beans.size()]);
	}

	protected InvokerBean parseInvoker(Element element) {
		if (Boolean.parseBoolean(element.attributeValue("enabled", "false"))) return null;
		else {
			logAsXml(element);
			String className = element.attributeValue("class");
			if (null == className || "".equals(className))
				throw new SystemException(Constants.UserError.CONFIG_ERROR, "Invoker elements need class attribute.");
			Class<? extends Invoker<?>> clazz = classForName(className);
			InvokerConfigBean config = InvokerFactory.getConfig(clazz);
			if (null != config) XMLUtils.setPropsByNode(config, element);
			logger.debug("Node [" + className + "] enabled.");
			return new InvokerBean(KeyUtils.objectId(), clazz, element.attributeValue("tx"), config,
					this.parseInvokerAuth(element));
		}
	}

	private Token parseInvokerAuth(Element element) {
		Element node = (Element) element.selectSingleNode("auth");
		if (node == null) return null;
		String token = node.attributeValue("token");
		if (token != null) return new Token(token);
		// TODO: suppurt key file (one key per line)
		// token = node.attributeValue("file");
		// if (token != null) {
		// BufferedReader r = new BufferedReader(new
		// InputStreamReader(Thread.currentThread().getContextClassLoader()
		// .getResourceAsStream(token)));
		// String line;
		// StringBuilder sb = new StringBuilder();
		// try {
		// while ((line = r.readLine()) != null)
		// sb.append(line).append("\n");
		// } catch (IOException e) {}
		// return new Token(sb.toString());
		// }
		String user = node.attributeValue("username");
		String pass = node.attributeValue("password");
		if (user != null && pass != null) return new Token(user, pass);
		return null;
	}

	public List<FilterBean> parseFilters(List<Element> filters) {
		List<FilterBean> list = new ArrayList<FilterBean>();
		for (Element filter : filters) {
			FilterBean f = parseFilter(filter);
			if (null != f) list.add(f);
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private FilterBean parseFilter(Element filter) {
		String title = filter.attributeValue("title");
		if (Boolean.parseBoolean(filter.attributeValue("enabled", "true"))) {
			Map<String, String> params = new HashMap<String, String>();
			for (Element param : (List<Element>) filter.selectNodes("param"))
				params.put(param.attributeValue("name"), param.attributeValue("value"));

			Class<? extends Filter> clazz;
			try {
				clazz = (Class<? extends Filter>) Class.forName(filter.attributeValue("class"));
			} catch (Throwable e) {
				throw new SystemException(Constants.UserError.FILTER_INVOKE, "Filter class invalid", e);
			}
			FilterBean f = new FilterBean(title, clazz, params);
			logger.debug("Filter [" + title + "] enabled.");
			logAsXml(filter);
			return f;
		} else {
			logger.trace("Filter [" + title + "] disabled.");
			logAsXml(filter);
			return null;
		}
	}

	protected static void logAsXml(Element element) {
		logger.trace(XMLUtils.format(element.asXML()).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "")
				.replaceAll("\n$$", ""));
	}

	@SuppressWarnings("unchecked")
	protected List<Element> elements(String xpath) {
		return root.selectNodes(xpath);
	}

	protected Element element(String xpath) {
		return (Element) root.selectSingleNode(xpath);
	}

	@SuppressWarnings("unchecked")
	protected RouterBean parseRouter() {
		Element element = this.element("router");
		if (element == null) return null;
		try {
			Class<? extends Router> routeClass = (Class<? extends Router>) Class.forName(element.attributeValue("class"));
			return new RouterBean(routeClass);
		} catch (Throwable th) {
			throw new SystemException(Constants.UserError.CONFIG_ERROR,
					"Route setting error: can't parse route/policy class name.", th);
		}
	}
}
