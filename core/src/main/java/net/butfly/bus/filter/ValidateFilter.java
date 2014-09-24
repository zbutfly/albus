package net.butfly.bus.filter;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Constants;
import net.butfly.bus.Constants.Side;
import net.butfly.bus.Request;
import net.butfly.bus.Response;

public class ValidateFilter extends FilterBase implements Filter {
	private Validator validator;

	@Override
	public Response execute(Request request) throws Exception {
		if (request.arguments() != null || request.arguments().length > 0) {
			Set<ConstraintViolation<Object>> violations;
			StringBuilder validateErrMsg = null;
			for (Object dto : request.arguments()) {
				violations = validator.validate(dto);
				Iterator<ConstraintViolation<Object>> it = violations.iterator();
				while (it.hasNext()) {
					validateErrMsg = validateErrMsg != null ? validateErrMsg : new StringBuilder();
					ConstraintViolation<Object> cv = it.next();
					validateErrMsg.append("[参数对象:" + cv.getRootBeanClass() + ",");
					validateErrMsg.append("属性:" + cv.getPropertyPath() + ",");
					validateErrMsg.append("错误信息:" + cv.getMessage() + "]");
				}
			}
			if (validateErrMsg != null) throw new SystemException(Constants.UserError.BAD_REQUEST, validateErrMsg.toString());
		}
		return super.execute(request);
	}

	@Override
	public void initialize(Map<String, String> params, Side side) {
		super.initialize(params, side);
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}
}
