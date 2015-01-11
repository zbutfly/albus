package net.butfly.bus.filter;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Request;
import net.butfly.bus.Response;
import net.butfly.bus.utils.Constants;
import net.butfly.bus.utils.RequestWrapper;

public class ValidateFilter extends FilterBase implements Filter {
	private Validator validator;

	@Override
	public Response execute(RequestWrapper<?> request) throws Exception {
		Request req = request.request();
		if (req.arguments() != null || req.arguments().length > 0) {
			Set<ConstraintViolation<Object>> violations;
			StringBuilder validateErrMsg = null;
			for (Object dto : req.arguments()) {
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
	public void initialize(Map<String, String> params) {
		super.initialize(params);
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}
}
