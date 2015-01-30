package net.butfly.bus.config.loader;

import java.io.InputStream;

public abstract class Loader {
	protected String configLocation;

	public String getConfigLocation() {
		return configLocation;
	}

	public Loader(String configLocation) {
		this.configLocation = configLocation;
	}

	public abstract InputStream load();
}
