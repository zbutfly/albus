package net.butfly.bus.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.butfly.albacore.utils.logger.Logger;

import net.butfly.bus.config.bean.FilterConfig;
import net.butfly.bus.config.bean.InvokerConfig;
import net.butfly.bus.config.bean.RouterConfig;

public class Configuration {
	protected final Logger logger = Logger.getLogger(this.getClass());
	private boolean debug;
	private Map<String, InvokerConfig> invokers = new HashMap<String, InvokerConfig>();
	private RouterConfig router;
	private List<FilterConfig> filterBeanList;

	public Configuration(boolean debug) {
		this.debug = debug;
	}

	public boolean debug() {
		return debug;
	}

	public InvokerConfig[] getInvokers() {
		return invokers.values().toArray(new InvokerConfig[0]);
	}

	public void setFilterList(List<FilterConfig> filterBeanList) {
		this.filterBeanList = filterBeanList;
	}

	public void setInvokers(InvokerConfig[] invokers) {
		for (InvokerConfig ivk : invokers)
			this.invokers.put(ivk.id(), ivk);
	}

	public FilterConfig[] getFilterList() {
		return filterBeanList.toArray(new FilterConfig[filterBeanList.size()]);
	}

	public RouterConfig getRouter() {
		return router;
	}

	public void setRouter(RouterConfig router) {
		this.router = router;
	}

	public String[] getAllNodeIDs() {
		return this.invokers.keySet().toArray(new String[this.invokers.keySet().size()]);
	}
}
