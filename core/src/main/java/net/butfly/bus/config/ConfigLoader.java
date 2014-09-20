package net.butfly.bus.config;

import java.io.InputStream;

public abstract class ConfigLoader {
	protected String configLocation;

	public String getConfigLocation() {
		return configLocation;
	}

	public ConfigLoader(String configLocation) {
		this.configLocation = configLocation;
	}

	public abstract InputStream load();
}
