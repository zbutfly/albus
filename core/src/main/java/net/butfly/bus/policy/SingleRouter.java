package net.butfly.bus.policy;


public class SingleRouter extends RouterBase implements Router {
	@Override
	protected <T> T route(T[] filted) {
		if (null == filted || filted.length != 1) return null;
		return filted[0];
	}
}
