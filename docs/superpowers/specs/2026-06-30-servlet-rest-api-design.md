# Servlet REST API 设计方案

## 概述

为座位预订系统搭建基于 Jakarta Servlet + JAX-RS (Jersey) 的 RESTful JSON API 服务端，以 WAR 包形式统一部署。

## 架构

### 模块结构

```
SeatReservationSystem (pom)
├── UserAndAuth      (jar) — 用户/认证业务
├── SeatAndRoom      (jar) — 座位/房间业务
├── Reservation      (jar) — 预订业务
├── SystemTask       (jar) — 系统任务
└── WebApi           (war) — Web 层，依赖以上所有模块  ← 新增
```

### 模块职责

- **WebApi**: 唯一的 WAR 模块，作为 API 入口。包含 JAX-RS Application 配置、Resource 类、Filter 类。依赖所有业务模块（jar），调用其
  Service/Domain 层。
- **UserAndAuth / SeatAndRoom / Reservation / SystemTask**: 纯业务模块（jar），不含 Web 层代码。提供
  Service、Repository、Domain 类供 WebApi 调用。

### 技术栈

| 组件         | 选型                                        | 说明                                                             |
|------------|-------------------------------------------|----------------------------------------------------------------|
| Servlet 容器 | Tomcat 10.1+                              | 通过 `maven-war-plugin` 打包 WAR，开发时用 `tomcat7-maven-plugin` 嵌入式运行 |
| JAX-RS 实现  | Jersey 3.x (`org.glassfish.jersey`)       | Jakarta EE 9+ 版本                                               |
| JSON 绑定    | Jackson，通过 `jersey-media-json-jackson` 集成 | JAX-RS 自动序列化 POJO 为 JSON                                       |
| 依赖注入       | HK2（Jersey 内置）                            | 轻量 DI，无需额外引入 Spring                                            |
| Java       | 17                                        | 与父 POM 一致                                                      |

## 目录结构

```
WebApi/src/main/
├── java/org/cleancoders/web/
│   ├── AppConfig.java          ← JAX-RS Application，注册 Resources 和 Providers
│   ├── filter/
│   │   └── CorsFilter.java     ← CORS 跨域过滤器
│   └── resource/
│       └── HealthResource.java ← 健康检查端点（首个 API）
├── resources/
│   └── (logback.xml)           ← 可选：日志配置
└── webapp/
    └── WEB-INF/
        └── web.xml             ← 声明 Jersey ServletContainer
```

## 核心组件设计

### 1. web.xml（最小化配置）

声明 Jersey `ServletContainer`，指向 `AppConfig`，映射 `/api/*` 路径：

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
    <servlet>
        <servlet-name>Jersey</servlet-name>
        <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
        <init-param>
            <param-name>jakarta.ws.rs.Application</param-name>
            <param-value>org.cleancoders.web.AppConfig</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>Jersey</servlet-name>
        <url-pattern>/api/*</url-pattern>
    </servlet-mapping>
</web-app>
```

### 2. AppConfig（JAX-RS Application）

继承 `Application`，注册所有 Resource 类和 Provider/Filter 类：

```java
// 注意：@ApplicationPath 与 web.xml 中的 <url-pattern> 二选一即可。
// 此处采用 web.xml 声明映射，因此 AppConfig 不加 @ApplicationPath。
public class AppConfig extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            HealthResource.class,
            CorsFilter.class
        );
    }
}
```

### 3. HealthResource（首个端点）

```java
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    @GET
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        )).build();
    }
}
```

端点：`GET /api/health` → `{"status": "UP", "timestamp": "2026-06-30T10:00:00Z"}`

### 4. CorsFilter（跨域过滤器）

实现 `ContainerResponseFilter`，为所有响应添加 CORS 头，允许前端跨域调用。

## Maven 依赖

`WebApi/pom.xml` 新增依赖：

| GroupId                           | ArtifactId                  | Scope   |
|-----------------------------------|-----------------------------|---------|
| `org.glassfish.jersey.containers` | `jersey-container-servlet`  | compile |
| `org.glassfish.jersey.inject`     | `jersey-hk2`                | compile |
| `org.glassfish.jersey.media`      | `jersey-media-json-jackson` | compile |

父 POM 已有 `jakarta.servlet-api`（provided），子模块自动继承。

`packaging` 从继承的 `jar` 改为 `war`，并引入 `maven-war-plugin`。

## 阶段划分

### 阶段 1（本次）：搭建骨架

- 新建 `WebApi` 模块，配置 POM
- 创建 `web.xml`、`AppConfig`、`HealthResource`、`CorsFilter`
- 验证 `GET /api/health` 返回正确 JSON 响应

### 阶段 2（后续）：业务 API

- 在 `UserAndAuth` 模块中实现用户注册/登录 API
- 在 `SeatAndRoom` 模块中实现座位/房间 CRUD API
- 在 `Reservation` 模块中实现预订 API
- 各 Resource 类通过模块间依赖调用业务层

## 错误处理

- Jersey 统一异常映射：注册 `ExceptionMapper<Throwable>` 将异常转为 JSON 错误响应
- 返回格式：`{"error": "message", "code": 400}`

## 测试

- 单元测试：每个 Resource 的 JAX-RS 端点通过 Jersey Test Framework 测试
- 集成测试：嵌入式 Tomcat 启动后发送 HTTP 请求验证