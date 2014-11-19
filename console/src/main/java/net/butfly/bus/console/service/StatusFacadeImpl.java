package net.butfly.bus.console.service;

import java.lang.reflect.Type;

import net.butfly.albacore.facade.FacadeBase;
import net.butfly.bus.console.facade.StatusFacade;

public class StatusFacadeImpl extends FacadeBase implements StatusFacade {
	private static final long serialVersionUID = 1631759045198147872L;

	@Override
	public String[] getTXs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getVersions(String tx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getReturnType(String code, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type[] getArguemtnTypes(String code, String version) {
		// TODO Auto-generated method stub
		return null;
	}
}
