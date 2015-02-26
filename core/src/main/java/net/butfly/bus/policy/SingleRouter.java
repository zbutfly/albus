package net.butfly.bus.policy;

public class SingleRouter extends RouterBase implements Router {
	@Override
	protected Routeable route(Routeable... filted) {
		if (null == filted || filted.length != 1) return null;
		return filted[0];
	}
}
