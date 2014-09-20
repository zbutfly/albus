package net.butfly.bus.dto;

import net.butfly.albacore.support.CloneSupport;

public abstract class DTOBase<T extends CloneSupport<T>> extends CloneSupport<T> {
	private static final long serialVersionUID = -2750322697249262868L;
}
