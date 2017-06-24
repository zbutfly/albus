package net.butfly.albacore.io;

import java.net.InetSocketAddress;

public class Udps {
	public static final int UDP_DIAGRAM_MAX_LEN = 0xFFFF - 8 - 20;
	public static final InetSocketAddress UDP_DEFAULT_SERV = new InetSocketAddress("127.0.0.1", 29900);
}
