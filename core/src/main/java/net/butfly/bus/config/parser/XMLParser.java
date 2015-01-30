package net.butfly.bus.config.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.more.XMLUtils;
import net.butfly.bus.Token;
import net.butfly.bus.config.Configuration;
import net.butfly.bus.config.bean.FilterConfig;
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.config.bean.RouterConfig;
import net.butfly.bus.filter.Filter;
import net.butfly.bus.invoker.Invoker;
import net.butfly.bus.policy.Router;
import net.butfly.bus.utils.Constants;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XMLParser extends Parser {
	protected Document document;
	protected Element root;

	@Override
	public Configuration parse() {
		boolean debug = Boolean.parseBoolean(root.attributeValue("debug", Boolean.toString(false)));
		Configuration config = new Configuration(debug);
		config.setFilterList(this.parseFilters(this.elements("filter")));
		config.setInvokers(this.parseInvokers());
		config.setRouter(this.parseRouter());

		return config;
	}

	public XMLParser(InputStream source) {
		super();
		if (null == source) throw new SystemException(Constants.UserError.CONFIG_ERROR, "StandardBus configurations invalid.");
		try {
			this.document = new SAXReader().read(source);
		} catch (DocumentException e) {
			throw new SystemException(Constants.UserError.CONFIG_ERROR, "StandardBus configurations invalid.", e);
		}
		this.root = this.document.getRootElement();
	}

	private InvokerConfig[] parseInvokers() {
		List<Element> elements = this.elements("invoker");
		List<InvokerConfig> beans = new ArrayList<InvokerConfig>();
		if (elements == null || elements.size() == 0)
			throw new SystemException(Constants.UserError.CONFIG_ERROR, "No invoker found, bus could not work.");
		for (Element element : elements) {
			InvokerConfig ivk = this.parseInvoker(element);
			if (null != ivk) beans.add(ivk);
		}
		return beans.toArray(new InvokerConfig[beans.size()]);
	}

	@SuppressWarnings("unchecked")
	private InvokerConfig parseInvoker(Element element) {
		if (Boolean.parseBoolean(element.attributeValue("enabled", "false"))) return null;
		else {
			logAsXml(element);
			Map<String, String> params = new HashMap<String, String>();
			for (Element node : (List<Element>) element.selectNodes("*")) {
				String name = node.getName();
				String value = node.getTextTrim();
				params.put(name, value);
			}
			Class<? extends Invoker> cl = Reflections.forClassName(element.attributeValue("class"));
			return new InvokerConfig(cl, params, element.attributeValue("tx"), this.parseInvokerAuth(element));
		}
	}

	private Token parseInvokerAuth(Element element) {
		Element node = (Element) element.selectSingleNode("auth");
		if (node == null) return null;
		String token = node.attributeValue("token");
		if (token != null) return new Token(token);
		// TODO: suppurt key file (one key per line)
		String user = node.attributeValue("username");
		String pass = node.attributeValue("password");
		if (user != null && pass != null) return new Token(user, pass);
		return null;
	}

	private List<FilterConfig> parseFilters(List<Element> filters) {
		List<FilterConfig> list = new ArrayList<FilterConfig>();
		for (Element filter : filters) {
			FilterConfig f = parseFilter(filter);
			if (null != f) list.add(f);
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private FilterConfig parseFilter(Element filter) {
		String title = filter.attributeValue("title");
		if (Boolean.parseBoolean(filter.attributeValue("enabled", "true"))) {
			Map<String, String> params = new HashMap<String, String>();
			for (Element param : (List<Element>) filter.selectNodes("param"))
				params.put(param.attributeValue("name"), param.attributeValue("value"));

			Class<? extends Filter> clazz = Reflections.forClassName(filter.attributeValue("class"));
			FilterConfig f = new FilterConfig(title, clazz, params);
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
		if (logger.isTraceEnabled())
			logger.trace(XMLUtils.format(element.asXML()).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "")
					.replaceAll("\n$$", ""));
	}

	@SuppressWarnings("unchecked")
	private List<Element> elements(String xpath) {
		return root.selectNodes(xpath);
	}

	private Element element(String xpath) {
		return (Element) root.selectSingleNode(xpath);
	}

	private RouterConfig parseRouter() {
		Element element = this.element("router");
		if (element == null) return null;
		Class<? extends Router> routeClass = Reflections.forClassName(element.attributeValue("class"));
		return new RouterConfig(routeClass);
	}
}
