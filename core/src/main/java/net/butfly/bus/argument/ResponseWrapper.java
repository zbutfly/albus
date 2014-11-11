package net.butfly.bus.argument;

import net.butfly.albacore.exception.SystemException;

public class ResponseWrapper extends Response {
	private static final long serialVersionUID = 4569901861887755671L;
	protected String resultClass;

	public ResponseWrapper(Response response) {
		this.id = response.id;
		this.requestId = response.requestId;
		this.result = response.result;
		this.context = response.context;
		this.error = response.error;
		this.resultClass(this.result.getClass());
	}

	public void resultClass(Class<?> returnType) {
		this.resultClass = returnType.getName();
	}

	public Class<?> resultClass() {
		try {
			return Class.forName(this.resultClass);
		} catch (ClassNotFoundException e) {
			throw new SystemException("", e);
		}
	}
}
