package net.butfly.bus.demo.facade;

import net.butfly.bus.TX;

public interface SampleFacade {
	@TX(value = "SPL_001")
	String echo(String str);
}
