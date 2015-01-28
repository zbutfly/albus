package net.butfly.bus.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.butfly.bus.config.bean.FilterBean;
import net.butfly.bus.config.bean.RouterBean;
import net.butfly.bus.config.bean.invoker.InvokerBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	private boolean debug;
	private Map<String, InvokerBean> invokers = new HashMap<String, InvokerBean>();
	private RouterBean router;
	private List<FilterBean> filterBeanList;

	public Config(boolean debug) {
		this.debug = debug;
	}

	public boolean debug() {
		return debug;
	}

	public InvokerBean[] getInvokers() {
		return invokers.values().toArray(new InvokerBean[0]);
	}

	public InvokerBean getInvoker(String invokerId) {
		return invokers != null ? invokers.get(invokerId) : null;
	}

	public void setFilterList(List<FilterBean> filterBeanList) {
		this.filterBeanList = filterBeanList;
	}

	public void setInvokers(InvokerBean[] invokers) {
		for (InvokerBean ivk : invokers)
			this.invokers.put(ivk.id(), ivk);
	}

	public FilterBean[] getFilterList() {
		return filterBeanList.toArray(new FilterBean[filterBeanList.size()]);
	}

	public RouterBean getRouter() {
		return router;
	}

	public void setRouter(RouterBean router) {
		this.router = router;
	}

	public String[] getAllNodeIDs() {
		return this.invokers.keySet().toArray(new String[this.invokers.keySet().size()]);
	}
}
