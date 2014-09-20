package net.butfly.bus.hessian;

import java.io.IOException;
import java.lang.reflect.Method;

import com.caucho.hessian.io.AbstractHessianInput;

public interface HessianSkeleton {
	Invoker getInvoker(AbstractHessianInput in, HessianExceptionHandler handler) throws IOException;

	public class Invoker {
		public Method method;
		public Object[] args;
	}
}
