package net.butfly.bus.policy;

public class SimpleRouter extends RouterBase implements Router {
	@Override
	protected <T> T route(T... filted) {
		if (null == filted || filted.length == 0) return null;
		return filted[(int) (Math.random() * filted.length)];
	}
}
