package net.butfly.bus.filter;

import net.butfly.albacore.exception.AuthenticationException;
import net.butfly.bus.service.AuthService;

public class AuthFilter extends FilterBase implements Filter {
	@Override
	public void execute(final FilterContext context) throws Exception {
		AuthService auth = context.invoker().awared(AuthService.class);
		// TODO: use a system prop to confirm auth filter
		if (null == auth) throw new AuthenticationException("AuthFilter defined, but auth bean could not be found.");
		auth.login(context.invoker().token());
		super.execute(context);
	}
}
