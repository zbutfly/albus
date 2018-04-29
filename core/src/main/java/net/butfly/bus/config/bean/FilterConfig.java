package net.butfly.bus.config.bean;

import java.util.Map;

import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.filter.Filter;

public class FilterConfig extends Config {
	private static final long serialVersionUID = 7823660786588727817L;

	private String title;

	private Filter instance;
	private Map<String, String> params;

	public FilterConfig(String title, Class<? extends Filter> clazz, Map<String, String> params) {
		this.title = title;
		this.params = params;
		this.instance = Reflections.construct(clazz);
	}

	public String getTitle() {
		return title;
	}

	public Filter getFilter() {
		return instance;
	}

	public Map<String, String> getParams() {
		return params;
	}
}
