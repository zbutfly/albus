package net.butfly.bus.config.loader;

import java.io.InputStream;

import net.butfly.bus.config.ConfigLoader;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class JDBCConfigLoad extends ConfigLoader {
	public JDBCConfigLoad(String jdbcConnURL) {
		super(jdbcConnURL);
		// DataSource ds = new DataSource();
	}

	@Override
	public InputStream load() {
		throw new NotImplementedException();
	}
}
