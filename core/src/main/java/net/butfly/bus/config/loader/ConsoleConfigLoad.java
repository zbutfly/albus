package net.butfly.bus.config.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import net.butfly.bus.config.ConfigLoader;

/**
 * load configuration of current bus node from another bus node (console node).
 */
public class ConsoleConfigLoad extends ConfigLoader {
	public ConsoleConfigLoad(String remoteConfigURL) {
		super(remoteConfigURL);
	}

	@Override
	public InputStream load() {
		URL url;
		try {
			url = new URL(configLocation);
		} catch (MalformedURLException e) {
			return null;
		}
		try {
			return url.openStream();
		} catch (IOException e) {
			return null;
		}
	}
}