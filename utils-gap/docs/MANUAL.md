# 杭州匡信网闸系统（CI-GAP）部署和运维手册

## 系统部署
解压<code>albus-util-gap-****.tar.gz</code>到任一目录即可完成部署

## 系统配置

#### albus-util-gap-http
dispatcher默认配置文件：
<code>dispatcher-default.properties</code>
配置项
```
bus.gap.dispatcher.src=./pool/responses # 监视此目录以解析响应
bus.gap.dispatcher.dst=./pool/requests  # 请求文件的存储目录
bus.gap.dispatcher.host=0.0.0.0         # dispatcher IP
bus.gap.dispatcher.port=28880           # gap监听的端口
bus.gap.dispatcher.method=POST,OPTION   # 支持的HTTP请求
```
invoker默认配置文件：
<code>invoker-default.properties</code>
配置项
```
bus.gap.invoker.src=./pool/requests     # 监视此目录以解析请求
bus.gap.invoker.dst=./pool/responses    # 响应文件的存储目录
bus.gap.invoker.host=10.118.128.114     # 实际服务器的IP
bus.gap.invoker.port=6060               # 实际监听的端口
bus.gap.invoker.method=POST,OPTION      # 支持的HTTP请求
```

参数亦可在运行时指定，此时，默认配置文件中的参数会被覆盖，具体可查看系统维护部分。
#### albus-util-gap-kcptun
参数在运行时指定，对于各参数的意义，请参考系统维护部分。

## 系统维护

#### albus-gap-http
1. 在内网启动Invoker
``` shell
java -d64 -server -cp ./test-classes;./albus-util-gap-http.jar;./dependency/* net.butfly.bus.utils.gap.Invoker <host:port> <dumping path> <watching path>

# <host:port>      实际http服务器的ip和端口
# <dumping path>   中间文件写入的目录
# <watching path>  监控的目录
```
2. 在外网启动Dispatcher
``` shell
java -d64 -server -cp ./test-classes;./albus-util-gap-http.jar;./dependency/* net.butfly.bus.utils.gap.Dispatcher <host:port> <dumping path> <watching path>

# <host:port>      虚拟http服务器的ip和端口
# <dumping path>   中间文件写入的目录
# <watching path>  监控的目录
```

#### albus-gap-kcptun
1. 在内网启动Invoker
``` shell
java -d64 -server -cp ./test-classes;./albus-util-gap-kcptun.jar;./dependency/* net.butfly.bus.utils.gap.Invoker <kcp server port> <dumping path> <watching path>
```
2. 在内网启动kcp-server
``` shell
# windows
.\kcptun\server_windows_amd64 -t "<really ip>:<really port>" -l ":<kcp listen port>" -mode fast2

# linux
./kcptun/server_linux_amd64.bin -t "<really ip>:<really port>" -l ":<kcp listen port>" -mode fast2 &
```

3. 在外网启动Dispatcher
``` shell
java -d64 -server -cp ./test-classes;./albus-util-gap-kcptun.jar;./dependency/* net.butfly.bus.utils.gap.Dispatcher <remote port> <dumping path> <watching path>
```
4. 在外网启动kcp-client
``` shell
# windows
.\kcptun\client_windows_amd64 -r "<remote loop ip>:<remote port>" -l ":<port for user>" -mode fast2

# linux
./kcptun/client_linux_amd64.bin -r "<remote loop ip>:<remote port>" -l ":<port for user>" -mode fast2 &
```
5. 示例
``` shell
#内网
# 1. start Invoker
java -d64 -server -cp ./test-classes;./albus-util-gap-kcptun.jar;./dependency/* net.butfly.bus.utils.gap.Invoker 29991 E:\pool\reqs E:\pool\reps
# 2. start kcp-server
.\kcptun\server_windows_amd64 -t "172.16.16.241:22" -l ":29991" -mode fast2

#外网
# 3. start Dispatcher
java -d64 -server -cp ./test-classes;./albus-util-gap-kcptun.jar;./dependency/* net.butfly.bus.utils.gap.Dispatcher 29992 E:\pool\reps E:\pool\reqs
# 4. start kcp-client
.\kcptun\client_windows_amd64 -r "127.0.0.1:29992" -l ":29990" -mode fast2

# 可以通过访问外外网的 29990 端口来访问内网服务器172.16.16.241:22，即ssh服务
# 5. access ssh on 29990
ssh root@172.16.16.116 29990
```



<b>注意：</b>需要定时清理中间文件，防止小文件过多影响文件系统性能
