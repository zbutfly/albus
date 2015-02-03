package net.butfly.bus.config.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ClasspathLoader extends Loader {
	public ClasspathLoader(String configLocation) {
		super(configLocation);
	}

	@Override
	public InputStream load() {
		if (null == this.configLocation) return null;
		URL url = Thread.currentThread().getContextClassLoader().getResource(this.configLocation);
		if (null == url) return null;
		try {
			return url.openStream();
		} catch (IOException e) {
			return null;
		}
	}
}
