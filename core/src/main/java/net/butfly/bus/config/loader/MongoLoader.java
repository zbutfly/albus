package net.butfly.bus.config.loader;

import java.io.InputStream;

import net.butfly.albacore.exception.NotImplementedException;

public class MongoLoader extends Loader {
	public MongoLoader(String mongoConnURL) {
		super(mongoConnURL);
		// DataSource ds = new DataSource();
	}

	@Override
	public InputStream load() {
		throw new NotImplementedException();
	}
}
