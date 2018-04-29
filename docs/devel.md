# ALBus架构设计

## 简单开发

### 添加依赖

```xml
<dependency>
    <groupId>net.butfly.bus</groupId>
        <artifactId>albus-plus</artifactId>
        <version>3.0.0-SNAPSHOT</version>
</dependency>
```

### 接口定义

最简单的albus接口由一个带@TX标注的接口方法定义，该接口应该继承Facade接口：

```java
public
interface SampleFacade {
   @TX(value = "SPL_001")
   String echo(String str);
}
```

### 业务单元定义

Albus需要接受一个业务单元（bean）组合。一般有两种模式：

- BeanFactory模式：自行创建所有业务单元实例。

```java
public
class BeanFactory extends AbstractBeanFactoryInvoker   {
   @Override
   public Object[] getBeanList() {
      return
new Object[] { new SampleFacadeImpl() };
   }
}
```

```xml
<?xmlversion="1.0"encoding="UTF-8"?>
<bus>
   <routerclass="net.butfly.bus.policy.SimpleRouter"/>
   <filtertitle="logger-filter"class="net.butfly.bus.filter.LoggerFilter"/>
   <routerclass="net.butfly.bus.policy.SimpleRouter"/>
   <invokertx="SPL_*"class="net.butfly.bus.demo.BeanFactory">
      <module>sample</module>
   </invoker>
</bus>
```

- Spring容器模式：由Spring托管所有业务单元实例。

```xml
<?xmlversion="1.0"encoding="UTF-8"?>
<bus>
   <routerclass="net.butfly.bus.policy.SimpleRouter"/>
   <filtertitle="logger-filter"class="net.butfly.bus.filter.LoggerFilter"/>
   <routerclass="net.butfly.bus.policy.SimpleRouter"/>
   <invokertx="SPL_*"class="net.butfly.bus.invoker.SpringInvoker">
      <module>sample</module>
      <files>beans-demo.xml</files>
   </invoker>
</bus>
```

- 客户端配置

```xml
<?xmlversion="1.0"encoding="UTF-8"?>
<busdebug="true">
   <filtertitle="logger-handler"class="net.butfly.bus.filter.LoggerFilter"/>
   <invokertx="*"class="net.butfly.bus.invoker.WebServiceInvoker">
      <path>http://127.0.0.1:19080/demo</path>
      <serializer>net.butfly.bus.serialize.HessianSerializer</serializer>
   </invoker>
</bus>
```
## 配置手册

Albus从配置文件中读取配置。默认配置文件为classpath下的bus.xml。建议分离客户端和服务端配置为bus-client.xml/bus-server.xml。

### 配置文件样例

```xml
<?xmlversion="1.0"encoding="UTF-8"?>
<busid="bus-person"side="server">
    <filtertitle="async-filter"class="net.butfly.bus.filter.AsyncFilter"
        enabled="false"/>
    <filtertitle="logger-filter"class="net.butfly.bus.filter.LoggerFilter"
        enabled="true"/>
    <filtertitle="exception-filter"class="net.butfly.bus.filter.ExceptionHandleFilter"
        enabled="true"/>
    <routertype="net.butfly.bus.policy.SimpleRouter"/>
    <invokerid="albus-test-comet"tx="API-PERSON_*"type="net.butfly.bus.invoker.SpringInvoker">
        <files>com/hzcominfo/appstore/services/person/spring/beans.xml</files>
        <lazy>false</lazy>
    </invoker>
</bus>
```

### 配置项

<table>
<tr><th>配置节点</th><th>配置节点说明</th><th>参数</th><th>参数说明</th></th>
<tr><td>bus</td><td>配置文件根节点</td><td>id</td><td>必须唯一</td></tr>
<tr><td>side</td><td>可选，server/client</td><td></td><td></td></tr>
<tr><td>filter</td><td>过滤器</td><td>title</td><td>必须唯一</td></tr>
<tr><td>class</td><td>过滤器实现类</td><td></td><td></td></tr>
<tr><td>enable</td><td>过滤器是否启用</td><td></td><td></td></tr>
<tr><td>invoker</td><td>总线调用定义</td><td>id</td><td>必须唯一</td></tr>
<tr><td>class</td><td>调用实现类</td><td></td><td></td></tr>
<tr><td>tx</td><td>由该调用器处理的TX码，逗号分隔，接受通配符（*）</td><td></td><td></td></tr>
</table>

### 调用配置

<table>
<tr><th>调用实现</th><th>调用实现说明</th><th>参数配置</th><th>参数说明</th></th>
<tr><td>BeanFactoryInvoker</td><td>简单对象集合调用</td><td>无需参数配置</td><td></td></tr>
<tr><td>SpringInvoker</td><td>Spring容器调用</td><td>file</td><td>Spring容器定义文件（基于classpath）</td></tr>
<tr><td>lazy</td><td>是否延迟加载spring上下文</td><td></td><td></td></tr>
<tr><td>WebServiceInvoker</td><td>WEB服务调用</td><td>path</td><td>WEB服务入口</td></tr>
</table>

## 部署和启动
### 本地调用
当albus客户端（接口调用者）配置中直接使用本地调用器（如BeanFactoryInvoker/SpringInvoker），albus实现本地接口调用模式，亦即，接口客户端和接口服务端在同一应用中。一般该模式用于开发调试或接口抽象。
### 客户端启动
当albus客户端（接口调用者）配置中使用远程调用器（如WebServiceInvoker），albus实现C/S调用模式。调用被发送到指定的服务端（接口实现者）。
### 服务端
服务端（接口实现者）可以在配置中使用本地调用器或远程调用器，前者在服务端实现了具体业务接口，后者实现了业务调用转发。
服务端可以使用标准Servlet容器（如Tomcat、Jetty、WebLogic等）。在容器中定义WebServiceServlet以开放接口。WebServiceServlet应带有参数config-file，该参数指定了albus配置文件入口（基于classpath）。
服务端也可以使用albus提供的嵌入容器，直接从命令行启动albus：

- 启动类（main函数所在类）：net.butfly.bus.deploy.JettyStarter
- 参数：albus配置文件
- 选项：

<table>
<tr><th>选项</th><th>说明</th></th>
<tr><td>-h, \-\-help</td><td>打印帮助</td></tr>
<tr><td>-s, \-\-secure</td><td>使用https（当前无效）</td></tr>
</table>

- 系统变量：

<table>
<tr><th>系统变量名</th><th>说明</th><th>默认值</th></th>
<tr><td>bus.port</td><td>服务端口</td><td>19080</td></tr>
<tr><td>bus.port.secure</td><td>是否使用SSL(当前无效)</td><td>19443</td></tr>
<tr><td>bus.threadpool.size</td><td>线程池大小</td><td>-1（不使用线程池）</td></tr>
<tr><td>bus.server.context</td><td>服务URL上下文（WEB应用名）</td><td>/bus/*</td></tr>
<tr><td>bus.jndi</td><td>JNDI资源定义文件（格式同tomcat的context.xml）</td><td>无jndi资源</td></tr>
<tr><td>bus.server.base</td><td>WEB静态文件根目录</td><td>无静态WEB资源</td></tr>
<tr><td>bus.server.class</td><td>Albus实现类</td><td>net.butfly.bus.Bus</td></tr>
<tr><td>bus.servlet.class</td><td>Albus接口Servlet类</td><td>net.butfly.bus.deploy.WebServiceServlet</td></tr>
</table>