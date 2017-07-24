package net.butfly.bus.utils.gap;

import net.butfly.albacore.utils.logger.Loggable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * 渡口, 码头
 */
public interface Wharf extends Loggable, Runnable {
    /**
     * 装载，把数据发送出去
     * @param key 数据 唯一ID
     * @param outing OutputStream为要发送的数据, 数据由在lambda中填充
     * @throws IOException e
     */
    void touch(String key, Consumer<OutputStream> outing) throws IOException;

    /**
     * 卸载，对收到的数据做处理
     * @param key 数据唯一ID
     * @param in 收到的数据
     */
    void seen(String key, InputStream in);
}
