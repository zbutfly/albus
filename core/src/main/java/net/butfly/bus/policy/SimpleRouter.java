package net.butfly.bus.policy;

public class SimpleRouter extends RouterBase implements Router {
	@Override
	protected Routeable route(Routeable... filted) {
		if (null == filted || filted.length == 0) return null;
		return filted[(int) (Math.random() * filted.length)];
	}
}
