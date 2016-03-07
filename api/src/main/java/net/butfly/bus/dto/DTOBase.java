package net.butfly.bus.dto;

<<<<<<< HEAD
import java.io.Serializable;

public abstract class DTOBase<T extends DTOBase<T>> implements Serializable {
=======
import net.butfly.albacore.support.Bean;
import net.butfly.albacore.support.Beans;

public abstract class DTOBase<T extends Beans<T>> extends Bean<T> implements Beans<T> {
>>>>>>> d6bdd690b57180f9538f7a61e655f27501aa8491
	private static final long serialVersionUID = -2750322697249262868L;
}
