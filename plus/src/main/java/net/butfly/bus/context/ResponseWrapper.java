package net.butfly.bus.context;

import java.lang.reflect.Type;

import net.butfly.albacore.utils.UtilsBase;
import net.butfly.bus.Response;

import com.google.common.reflect.TypeToken;

public class ResponseWrapper extends UtilsBase {
	public static Class<? extends Response> wrapClass(boolean wrap) {
		return wrap ? Response.class : WrappedResponse.class;
	}

	public static Response wrap(Response resp, boolean wrap) {
		return wrap ? resp : new WrappedResponse(resp);
	}

	public static Type unwrap(Response resp) {
		if (null != resp.result() && resp instanceof WrappedResponse) {
			Type expected = ((WrappedResponse) resp).resultClass();
			return expected;
		}
		return null;

	}

	static class WrappedResponse extends Response {
		private static final long serialVersionUID = 4569901861887755671L;
		protected String resultClass;

		public WrappedResponse(Response response) {
			this.id = response.id();
			this.requestId = response.requestId();
			this.result = response.result();
			this.context = response.context();
			this.error = response.error();
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

}
