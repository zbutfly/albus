package net.butfly.bus.argument;

import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;

public class ResponseWrapper extends Response {
	private static final long serialVersionUID = 4569901861887755671L;
	protected String resultClass;

	public ResponseWrapper(Response response) {
		this.id = response.id;
		this.requestId = response.requestId;
		this.result = response.result;
		this.context = response.context;
		this.error = response.error;
		if (this.result != null) this.resultClass = TypeToken.of(this.result.getClass()).toString();
		else this.resultClass = null;
	}

	public Type resultClass() {
		try {
			return Class.forName(this.resultClass);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
