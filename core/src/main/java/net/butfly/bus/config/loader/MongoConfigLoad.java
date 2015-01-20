package net.butfly.bus.config.loader;

import java.io.InputStream;

import net.butfly.bus.config.ConfigLoader;
import net.butfly.albacore.exception.NotImplementedException;

public class MongoConfigLoad extends ConfigLoader {
	public MongoConfigLoad(String mongoConnURL) {
		super(mongoConnURL);
		// DataSource ds = new DataSource();
	}

	@Override
	public InputStream load() {
		throw new NotImplementedException();
	}
}
