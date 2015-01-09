package net.butfly.bus.config.bean;

import java.util.Map;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.filter.Filter;
import net.butfly.bus.utils.Constants;

public class FilterBean extends ConfigBean {
	private static final long serialVersionUID = 7823660786588727817L;

	private String title;

	private Filter instance;
	private Map<String, String> params;

	public FilterBean(String title, Class<? extends Filter> clazz, Map<String, String> params) {
		this.title = title;
		this.params = params;
		try {
			this.instance = clazz.newInstance();
		} catch (Throwable ex) {
			throw new SystemException(Constants.UserError.FILTER_INVOKE, "Filter [" + clazz.getName()
					+ "] instance creation failed .");
		}
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
