package net.butfly.bus.invoker;

public class ParameterInfo {
	private Class<?>[] parametersTypes;
	private Class<?> returnType;

	public ParameterInfo(Class<?>[] parametersTypes, Class<?> returnType) {
		super();
		this.parametersTypes = parametersTypes;
		this.returnType = returnType;
	}

	public Class<?>[] parametersTypes() {
		return parametersTypes;
	}

	public Class<?> returnType() {
		return returnType;
	}
}
