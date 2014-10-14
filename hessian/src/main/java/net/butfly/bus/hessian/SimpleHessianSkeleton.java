package net.butfly.bus.hessian;

import java.io.IOException;
import java.lang.reflect.Method;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.services.server.AbstractSkeleton;
import com.caucho.services.server.ServiceContext;

/**
 * Proxy class for HessianSerializer services.
 */
public class SimpleHessianSkeleton extends AbstractSkeleton implements HessianSkeleton {
	public SimpleHessianSkeleton(Class<?> apiClass) {
		super(apiClass);
	}

	@Override
	public Invoker getInvoker(AbstractHessianInput in, HessianExceptionHandler handler) throws IOException {
		ServiceContext context = ServiceContext.getContext();

		// backward compatibility for some frameworks that don't read
		// the call type first
		in.skipOptionalCall();

		// HessianSerializer 1.0 backward compatibility
		String header;
		while ((header = in.readHeader()) != null) {
			Object value = in.readObject();

			context.addHeader(header, value);
		}

		String methodName = in.readMethod();
		int argLength = in.readMethodArgLength();

		Method method;

		method = getMethod(methodName + "__" + argLength);

		if (method == null) method = getMethod(methodName);

		if (method == null) {
			handler.handle("NoSuchMethodException", "The service has no method named: " + in.getMethod(), null);
			return null;
		}

		Class<?>[] args = method.getParameterTypes();

		if (argLength != args.length && argLength >= 0) {
			handler.handle("NoSuchMethod", "method " + method + " argument length mismatch, received length=" + argLength, null);
			return null;
		}

		Object[] values = new Object[args.length];

		for (int i = 0; i < args.length; i++)
			values[i] = in.readObject(args[i]);
		Invoker ivk = new Invoker();
		ivk.method = method;
		ivk.args = values;
		return ivk;
	}
}
