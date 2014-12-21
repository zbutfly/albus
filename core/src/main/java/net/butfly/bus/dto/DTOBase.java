package net.butfly.bus.dto;

import net.butfly.albacore.support.BaseObjectSupport;
import net.butfly.albacore.support.ObjectSupport;

public abstract class DTOBase<T extends ObjectSupport<T>> extends BaseObjectSupport<T> implements ObjectSupport<T> {
	private static final long serialVersionUID = -2750322697249262868L;
}
