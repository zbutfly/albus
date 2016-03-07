package net.butfly.bus.dto;

import net.butfly.albacore.support.Bean;
import net.butfly.albacore.support.Beans;

public abstract class DTOBase<T extends Beans<T>> extends Bean<T> implements Beans<T> {
	private static final long serialVersionUID = -2750322697249262868L;
}
