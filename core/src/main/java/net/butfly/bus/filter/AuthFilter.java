package net.butfly.bus.filter;

import java.util.Map;

import net.butfly.bus.service.AuthService;

public class AuthFilter extends FilterBase implements Filter {
	private AuthService authService;

	@Override
	public void initialize(Map<String, String> params) {
		super.initialize(params);
		if (null == params || !params.containsKey("mode") || "BUS".equals(params.get("mode"))) {

		}
	}
}
