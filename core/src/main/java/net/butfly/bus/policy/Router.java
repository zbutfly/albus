package net.butfly.bus.policy;

public interface Router {
<<<<<<< HEAD
	@SuppressWarnings("unchecked")
=======
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
	<T extends Routeable> T route(String requestTX, T... possiable);
}
