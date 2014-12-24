package net.butfly.bus.dto;

import net.butfly.albacore.support.BasicBean;
import net.butfly.albacore.support.Beanable;

public abstract class DTOBase<T extends Beanable<T>> extends BasicBean<T> implements Beanable<T> {
	private static final long serialVersionUID = -2750322697249262868L;
}
