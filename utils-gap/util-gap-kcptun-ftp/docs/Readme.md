
## 编译发布
```bash
mvn package --settings path/to/your/maven/conf/dir/settings.xml dependency:copy-dependencies

mv target util-gap-kcptun-ftp
cd util-gap-kcptun-ftp 
rm -rf generated-sources  generated-test-sources maven-archiver maven-status
tar zcvf util-gap-kcptun-ftp.tar.gz util-gap-kcptun-ftp
```

## 如何安装
```bash
tar zxvf util-gap-kcptun-ftp.tar.gz
cd util-gap-kcptun-ftp
```

## 使用示例
### 场景
使用外网 172.16.16.116:29990 访问内网 172.16.16.242:22
### 配置
classes/invoker-default.properties
```properties
bus.gap.invoker.kcp.port=29991                     # invoker端kcptun通讯端口，需要与kcptun_server的启动参数'-l'指定的端口一致
bus.gap.invoker.ftp.server.properties.filename=invoker-users.properties             # invoker端启动ftp server的用户配置文件
bus.gap.invoker.ftp.server=172.16.16.116:2222      # invoker端ftp服务开启的ip和port
bus.gap.invoker.ftp.remote=172.16.16.116:2221      # dispatcher端ftp服务开启的ip和port
bus.gap.invoker.ftp.account=admin:admin            # invoker在连接dispatcher端ftp服务器使用的用户名和密码
```

classes/dispatcher-default.properties
```properties
bus.gap.dispatcher.kcp.port=29992                  # dispatcher端kcptun通讯端口，需要与kcptun_client的启动参数'-r'指定的端口一致
bus.gap.dispatcher.ftp.server.properties.filename=dispatcher-users.properties       # dispatcher端启动ftp server的用户配置文件
bus.gap.dispatcher.ftp.server=172.16.16.116:2221   # invoker端ftp服务开启的ip和port
bus.gap.dispatcher.ftp.remote=172.16.16.116:2222   # dispatcher端ftp服务开启的ip和port
bus.gap.dispatcher.ftp.account=admin:admin         # invoker在连接dispatcher端ftp服务器使用的用户名和密码
```

test-classes/log4j.xml
一般只需要修改第10行`<level value="debug" />`中的value值


### 启动
```bash
# 1. 在内网启动Invoker
java -d64 -server -cp ./test-classes;/classes;./albus-util-gap-kcptun-ftp.jar;./dependency/* net.butfly.bus.utils.gap.Invoker
# 2. 在外网启动Dispatcher
java -d64 -server -cp ./test-classes;/classes;./albus-util-gap-kcptun-ftp.jar;./dependency/* net.butfly.bus.utils.gap.Dispatcher
# 3. 在内网启动kcptun_server
.\kcptun\server_windows_amd64 -t "172.16.16.242:22" -l ":29991" -mode fast2
# 4. 在外网启动kcptun_client
.\kcptun\client_windows_amd64 -r "127.0.0.1:29992" -l ":29990" -mode fast2

# 5. 等待kcptun server 出现'remote address: 127.0.0.1:xxxx', 'stream opened',即可使用
ssh root@172.16.16.116 29990
```
