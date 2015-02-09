package net.butfly.bus.utils;

import javax.annotation.concurrent.Immutable;

@Immutable
public interface Constants {
	public static final String _PREFIX = "BUS_";

	public interface SystemError {// 000-099
		String UNKNOW_CAUSE = _PREFIX + "000";// 未知异常
		String CONFIG_ERROR = _PREFIX + "001";// 配置错误
		String HESSIAN_UNKNOW = _PREFIX + "002";// Hessian未知异常
		String HESSIAN_MALFORMED_URL = _PREFIX + "003";// 调用Hessian时应用了错误的URL格式
		String HESSIAN_CONNECTION = _PREFIX + "004";// Hessian连接错误
		String ROUTE_NOT_FOUND = _PREFIX + "005";// 路由节点失败

		// new thread control
		String NOT_IMPLEMENTED = _PREFIX + "091";
		String ENUM_ERROR = _PREFIX + "094";
	}

	public interface UserError {// 100-199
		String BAD_REQUEST = _PREFIX + "100";// 请求错误
		String USER_NOT_EXIST = _PREFIX + "101";// 用户不存在
		String WRONG_PASSWORD = _PREFIX + "102";// 密码错误
		String NODE_CONFIG = _PREFIX + "104";// 节点配置错误
		String NODE_NOT_FOUND = _PREFIX + "105";// 没有找到配置的节点
		String NODE_CONNECTION_NOT_FOUND = _PREFIX + "106";// 节点没有配置连接
		String NODE_INVOKER_NOT_FOUND = _PREFIX + "116";
		String TX_CONFIG = _PREFIX + "107";// 交易配置错误
		String TX_NOT_FOUND = _PREFIX + "108";// 交易没有找到
		String TX_NOT_EXIST = _PREFIX + "109";// 交易不存在
		String FILTER_CONFIG = _PREFIX + "111";// 过滤器配置错误
		String FILTER_INSTANCE = _PREFIX + "112";// 过滤器实例化错误
		String FILTER_INIT = _PREFIX + "113";// 过滤器初始化错误
		String FILTER_INVOKE = _PREFIX + "114";// 过滤器调用错误
		String CONFIG_ERROR = _PREFIX + "115";// 文件配置错误
	}

	public interface BusinessError {// 200-299
		String CONFIG_ERROR = _PREFIX + "200";// 业务逻辑配置错误
		String BUSI_INSTANCE = _PREFIX + "201";// 实例化错误
		String INVOKE_TIMEOUT = _PREFIX + "202";// 调用超时
		String INVOKE_ERROR = _PREFIX + "203";// 调用错误
		String BUSI_NOT_FOUND = _PREFIX + "204";// 业务逻辑不存在
		String SIMULATE_ERROR = _PREFIX + "299";// 业务模拟错误
		String TCP_ERROR = _PREFIX + "210"; // TCP错误

		String AUTH_NOT_EXIST = _PREFIX + "300";
		String AUTH_TOKEN_INVALID = _PREFIX + "301";
		String AUTH_USER_INVALID = _PREFIX + "302";
		String AUTH_PASS_INVALID = _PREFIX + "303";

		String SIGNAL = _PREFIX + 999; // Async signal code.
	}

	public interface Async {
		public final long DEFAULT_TIMEOUT = 5000;
		public final long INFINITE_TIMEOUT = -1;
		public final int DEFAULT_CORE_POOL_SIZE = 10;
		public final int DEFAULT_MAX_POOL_SIZE = 50;
		public final long DEFAULT_ALIVE_TIME = 5000;
	}

	public interface Configuration {
		public final String DEFAULT_SERVER_CONFIG = "bus-server.xml";
		public final String DEFAULT_CLIENT_CONFIG = "bus-client.xml";
		public final String DEFAULT_COMMON_CONFIG = "bus.xml";
		public final String INTERNAL_SERVER_CONFIG = "net/butfly/bus/" + DEFAULT_SERVER_CONFIG;
		public final String INTERNAL_CLIENT_CONFIG = "net/butfly/bus/" + DEFAULT_CLIENT_CONFIG;
		public final String INTERNAL_COMMON_CONFIG = "net/butfly/bus/" + DEFAULT_COMMON_CONFIG;
	}
}
