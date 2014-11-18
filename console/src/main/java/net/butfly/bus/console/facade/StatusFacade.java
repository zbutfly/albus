package net.butfly.bus.console.facade;

import java.lang.reflect.Type;

import net.butfly.albacore.facade.Facade;
import net.butfly.bus.argument.TX;

public interface StatusFacade extends Facade {
	@TX("BUS-001")
	String[] getTXs();

	@TX("BUS-002")
	String[] getVersions(String tx);

	@TX("BUS-003")
	Type getReturnType(String code, String version);

	@TX("BUS-004")
	Type[] getArguemtnTypes(String code, String version);
}
