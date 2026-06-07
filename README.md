# ServerTools

ServerTools 是一个独立的 Paper 插件项目，放在当前仓库的 `paper-plugin/` 目录中。它和根目录的 NanoLimbo 独立主程序互不影响。

插件版只负责启动辅助运行时：

- Java 源码版哪吒 Agent
- Cloudflare Tunnel Runtime
- WebSocket 代理/订阅服务

插件版不会启动 NanoLimbo Server，也不会阻塞 Paper 主线程。Paper 会正常完成启动，插件在后台线程运行自己的服务。

## 项目结构

```text
paper-plugin/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradle/
├── servertools.properties
├── README.md
└── src/main/
    ├── java/
    │   ├── App.java
    │   ├── HardcodedConfig.java
    │   ├── NezhaAgentBridge.java
    │   ├── SubscriptionComposer.java
    │   ├── TunnelSupport.java
    │   ├── Socks5Request.java
    │   ├── com/nezhahq/agent/...
    │   └── ua/nanit/servertools/PluginMain.java
    └── resources/
        └── plugin.yml
```

## 配置方式

编辑：

```text
servertools.properties
```

这个文件是明文配置文件，会在构建时被 Gradle 加密生成到 Java 类中。构建后的 `ServerTools.jar` 不需要再携带这个 properties 文件。

常用配置：

```properties
uuid=00000000-0000-0000-0000-000000000000

nezha.server=
nezha.port=
nezha.key=

tunnel.enabled=false
tunnel.token=
tunnel.domain=
tunnel.port=8001
```

完整配置项见 [servertools.properties](servertools.properties)。

## 构建

进入插件目录：

```bash
cd paper-plugin
./gradlew shadowJar
```

产物：

```text
paper-plugin/build/libs/ServerTools.jar
```

## 安装

把 `ServerTools.jar` 放入 Paper 服务端的 `plugins/` 目录，然后正常启动 Paper。

插件信息：

```yaml
name: ServerTools
main: ua.nanit.servertools.PluginMain
api-version: '1.20'
```

## 生命周期

- `onEnable()`：启动后台运行时线程
- `onDisable()`：中断后台运行时线程，触发 App 里的清理逻辑

注意：当前第一版复用了 `App.java` 的阻塞式运行逻辑，关闭行为依赖线程中断触发清理。后续如果需要更优雅的关闭，可以把 `App.java` 重构成 `JavaWsRuntime implements AutoCloseable`。

## Java 版本

建议使用 Java 21。

Paper 1.20.6+ 要求 Java 21；使用更高版本 Java 可能会看到 gRPC / Netty 的 native access warning。
