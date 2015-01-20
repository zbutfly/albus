package net.butfly.bus.impl;

public abstract class ContainerBase<T> implements Container<T> {
	private T impl;

	public final T impl() {
		return impl;
	}
}
