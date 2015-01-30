package net.butfly.bus.config.loader;

import java.io.InputStream;

import net.butfly.albacore.exception.NotImplementedException;

public class JDBCLoader extends Loader {
	public JDBCLoader(String jdbcConnURL) {
		super(jdbcConnURL);
		// DataSource ds = new DataSource();
	}

	@Override
	public InputStream load() {
		throw new NotImplementedException();
	}
}
