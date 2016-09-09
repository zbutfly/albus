package net.butfly.bus.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import net.butfly.albacore.exception.ValidateException;
import net.butfly.bus.Request;

public class ValidateFilter extends FilterBase implements Filter {
	private Validator validator;

	@Override
	public void initialize(Map<String, String> params) {
		super.initialize(params);
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Override
	public void execute(final FilterContext context) throws Exception {
		Request request = context.request();
		List<ConstraintViolation<?>> violations = new ArrayList<ConstraintViolation<?>>();
		if (request.arguments() != null || request.arguments().length > 0) {
			for (Object dto : request.arguments())
				violations.addAll(validator.validate(dto));
			if (violations.size() > 0)
				throw new ValidateException(violations.toArray(new ConstraintViolation<?>[violations.size()]));
		}
		super.execute(context);
	}
}
