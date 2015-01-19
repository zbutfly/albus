package net.butfly.bus.filter;

import net.butfly.bus.service.AuthService;

public class AuthFilter extends FilterBase implements Filter {
	@Override
	public void before(FilterContext context) {
		AuthService auth = context.invoker().authBean();
		if (null == auth) return;
	}
}
