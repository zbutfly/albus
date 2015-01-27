package net.butfly.bus.service;

import net.butfly.albacore.service.Service;

@AwareService
public interface LogService extends Service {
	void logAccess();
}
