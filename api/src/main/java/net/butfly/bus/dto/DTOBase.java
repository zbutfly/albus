package net.butfly.bus.dto;

import java.io.Serializable;

import net.butfly.albacore.support.Beans;

public abstract class DTOBase<T extends DTOBase<T>> implements Serializable, Beans<DTOBase<T>> {
	private static final long serialVersionUID = -2750322697249262868L;
}
