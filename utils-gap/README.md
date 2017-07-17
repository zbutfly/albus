# 杭州匡信网闸系统（CI-GAP）

## 介绍
CI-GAP是为了解决公安内网无法访问互联网而开发的工具，其原理为把一端的网络请求转换为文件，
经内外网文件同步，在另一端把文件转换为网络请求，再重定向到指定服务器上，对于接收到的响应，
也转换为文件，并做同步，再在本端转换为网络响应。至此，一次完整的网络请求便完成了。
```
              Dispatcher                      │ GAP │                     Invoker
 ┌────────┐    ┌────────┐   request   ┌──────┐│     │┌──────┐    request     ┌────────┐    ┌────────┐
 │        │ ←→ │        │ ----------→ │ file ││ --→ ││ file │  ----------→   │        │ ←→ │        │
 │ Really │ ←→ │ virtual│             └──────┘│     │└──────┘                │ virtual│ ←→ │ Really │
 │        │ ←→ │        │                     │     │                        │        │ ←→ │        │
 │ Client │ ←→ │ Client │             ┌──────┐│     │┌──────┐                │ Server │ ←→ │ Server │
 │        │ ←→ │        │ ←---------- │ file ││ ←-- ││ file │  ←----------   │        │ ←→ │        │
 └────────┘    └────────┘   response  └──────┘│     │└──────┘    response    └────────┘    └────────┘
                                              │     │
```

## 代码结构

```
albus-gap
   |--albus-util-gap-common  # gap架构，包含文件监控Watcher、Waiter等，不包含可执行文件
   |--albus-util-gap-http    # 实现http协议传输
   |--albus-util-gap-kcptun  # 基于kcptun实现TCP协议传输
   |--albus-util-gap-udp     # 实现udp协议传输
```

## 编译发布

1. 进入子模块所在目录，使用如下命令编译模块，得到目标jar包及其依赖：
``` shell
mvn clean package dependency:copy-dependencies
```
2. 编译完成后得到<code>target</code>目录，进入此目录，删除<code>classes</code>、
<code>generated-sources</code>、<code>maven-archiver</code>、<code>maven-
status</code>、<code>albus-util-gap-***-sources.jar</code>，
3. 打包发布
```
cd ..
mv target albus-util-gap-***
tar zcvf albus-tuil-gap-***.tar.gz albus-util-gap-***
```



## 文档引用

- [《部署和运维手册》](./docs/MANUAL.md)


## TODO

- 添加基于本机唯一ID的License授权机制