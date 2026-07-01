# Servlet REST API 骨架搭建 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建 WebApi 模块（WAR），搭建 Jersey + Jakarta Servlet REST API 骨架，实现 `GET /api/health` 健康检查端点。

**Architecture:** 新增 WebApi 子模块（WAR packaging），通过 Jersey 3.1.7 + Jakarta Servlet 6.1 提供 JAX-RS 风格的 JSON
API。web.xml 声明 ServletContainer，AppConfig 注册 Resource 和 Filter。

**Tech Stack:** Jakarta Servlet 6.1, Jersey 3.1.7, Jackson (via jersey-media-json-jackson), HK2 DI, Java 17, Maven WAR
Plugin 3.4.0

## Global Constraints

- Java 17 (`maven.compiler.source`, `maven.compiler.target`)
- `jakarta.servlet-api` 6.1.0 (provided scope，继承自父 POM)
- Jersey 3.1.x（Jakarta EE 10 兼容版本）
- 包路径 `org.cleancoders.web`，子包 `filter`、`resource`
- WAR 打包，内嵌 web.xml

---

## 文件结构

```
WebApi/
├── pom.xml                                          ← 新建：WAR 模块 POM
└── src/
    ├── main/
    │   ├── java/org/cleancoders/web/
    │   │   ├── AppConfig.java                       ← 新建：JAX-RS Application
    │   │   ├── filter/
    │   │   │   └── CorsFilter.java                  ← 新建：CORS 过滤器
    │   │   └── resource/
    │   │       └── HealthResource.java              ← 新建：健康检查端点
    │   └── webapp/
    │       └── WEB-INF/
    │           └── web.xml                          ← 新建：Servlet 声明
    └── test/
        └── java/org/cleancoders/web/
            └── HealthResourceTest.java              ← 新建：Jersey Test 集成测试

pom.xml                                              ← 修改：添加 WebApi 模块
```

---

### Task 1: 创建 WebApi 模块 POM 并更新父 POM

**Files:**

- Create: `WebApi/pom.xml`
- Modify: `pom.xml:10-16`

**Interfaces:**

- Consumes: 父 POM 中的 `jakarta.servlet-api:6.1.0`（provided）
- Produces: 无（后续 Task 依赖此模块存在且可编译）

- [ ] **Step 1: 更新父 POM 添加 WebApi 模块**

在 `pom.xml` 的 `<modules>` 块末尾新增：

```xml
<module>WebApi</module>
```

编辑前：

```xml
<modules>
    <module>UserAndAuth</module>
    <module>SeatAndRoom</module>
    <module>Reservation</module>
    <module>SystemTask</module>
</modules>
```

编辑后：

```xml
<modules>
    <module>UserAndAuth</module>
    <module>SeatAndRoom</module>
    <module>Reservation</module>
    <module>SystemTask</module>
    <module>WebApi</module>
</modules>
```

- [ ] **Step 2: 创建 WebApi/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.cleancoders</groupId>
        <artifactId>SeatReservationSystem</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>WebApi</artifactId>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Jersey Servlet Container -->
        <dependency>
            <groupId>org.glassfish.jersey.containers</groupId>
            <artifactId>jersey-container-servlet</artifactId>
            <version>3.1.7</version>
        </dependency>

        <!-- Jersey HK2 Dependency Injection -->
        <dependency>
            <groupId>org.glassfish.jersey.inject</groupId>
            <artifactId>jersey-hk2</artifactId>
            <version>3.1.7</version>
        </dependency>

        <!-- Jersey JSON Jackson -->
        <dependency>
            <groupId>org.glassfish.jersey.media</groupId>
            <artifactId>jersey-media-json-jackson</artifactId>
            <version>3.1.7</version>
        </dependency>

        <!-- Jersey Test Framework (test scope, provider grizzly) -->
        <dependency>
            <groupId>org.glassfish.jersey.test-framework.providers</groupId>
            <artifactId>jersey-test-framework-provider-grizzly</artifactId>
            <version>3.1.7</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 创建目录结构**

```bash
mkdir -p WebApi/src/main/java/org/cleancoders/web/filter
mkdir -p WebApi/src/main/java/org/cleancoders/web/resource
mkdir -p WebApi/src/main/webapp/WEB-INF
mkdir -p WebApi/src/test/java/org/cleancoders/web
```

- [ ] **Step 4: 验证 Maven 解析成功**

```bash
mvn validate
```

预期：BUILD SUCCESS，WebApi 模块被识别。

- [ ] **Step 5: Commit**

```bash
git add pom.xml WebApi/pom.xml
git commit -m "feat: add WebApi module with Jersey dependencies"
```

---

### Task 2: 创建 web.xml

**Files:**

- Create: `WebApi/src/main/webapp/WEB-INF/web.xml`

**Interfaces:**

- Consumes: `AppConfig` 类（Task 3 创建）
- Produces: Jersey ServletContainer 配置，映射 `/api/*`

- [ ] **Step 1: 创建 web.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6.0.xsd"
         version="6.0">

    <display-name>Seat Reservation System API</display-name>

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

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/webapp/WEB-INF/web.xml
git commit -m "feat: add web.xml with Jersey ServletContainer config"
```

---

### Task 3: 创建 AppConfig.java

**Files:**

- Create: `WebApi/src/main/java/org/cleancoders/web/AppConfig.java`

**Interfaces:**

- Consumes: `HealthResource.class`, `CorsFilter.class`（后续 Task 创建，此处先以注释说明）
- Produces: JAX-RS Application 注册类，供 `web.xml` 中 `jakarta.ws.rs.Application` 参数引用

- [ ] **Step 1: 创建 AppConfig.java**

```java
package org.cleancoders.web;

import jakarta.ws.rs.core.Application;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.resource.HealthResource;

import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application configuration.
 * Registers all resource classes and providers.
 * Path mapping is handled by web.xml, so no @ApplicationPath annotation here.
 */
public class AppConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Resources
        classes.add(HealthResource.class);
        // Providers / Filters
        classes.add(CorsFilter.class);
        return classes;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/AppConfig.java
git commit -m "feat: add AppConfig JAX-RS application class"
```

---

### Task 4: 创建 HealthResource.java

**Files:**

- Create: `WebApi/src/main/java/org/cleancoders/web/resource/HealthResource.java`

**Interfaces:**

- Consumes: 无
- Produces: `GET /api/health` — 返回 `{"status": "UP", "timestamp": "<ISO-8601>"}`

- [ ] **Step 1: 创建 HealthResource.java**

```java
package org.cleancoders.web.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoint.
 * GET /api/health returns service status.
 */
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

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/resource/HealthResource.java
git commit -m "feat: add HealthResource health check endpoint"
```

---

### Task 5: 创建 CorsFilter.java

**Files:**

- Create: `WebApi/src/main/java/org/cleancoders/web/filter/CorsFilter.java`

**Interfaces:**

- Consumes: 无
- Produces: 实现 `ContainerResponseFilter`，为所有响应添加 CORS 头（`Access-Control-Allow-Origin`,
  `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`）

- [ ] **Step 1: 创建 CorsFilter.java**

```java
package org.cleancoders.web.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * CORS filter that adds cross-origin headers to every response.
 * Registered as a JAX-RS @Provider, automatically discovered if listed in AppConfig
 * or if package scanning is enabled. Here we use explicit registration via AppConfig.
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        responseContext.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        responseContext.getHeaders().add("Access-Control-Allow-Headers",
                "Origin, Content-Type, Accept, Authorization");
        responseContext.getHeaders().add("Access-Control-Expose-Headers",
                "Location, Content-Disposition");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/filter/CorsFilter.java
git commit -m "feat: add CorsFilter for cross-origin support"
```

---

### Task 6: 编写集成测试

**Files:**

- Create: `WebApi/src/test/java/org/cleancoders/web/HealthResourceTest.java`

**Interfaces:**

- Consumes: `HealthResource`, `AppConfig`, `CorsFilter`（所有以上类）
- Produces: 验证端点返回 200 和正确 JSON body

- [ ] **Step 1: 创建 HealthResourceTest.java**

```java
package org.cleancoders.web;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.resource.HealthResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for the health check endpoint.
 * Uses Jersey Test Framework with embedded Grizzly HTTP server.
 */
public class HealthResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(HealthResource.class, CorsFilter.class);
    }

    @Test
    public void healthEndpoint_ShouldReturn200_WithStatusUP() {
        Response response = target("/health")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());

        String body = response.readEntity(String.class);
        assertNotNull(body);
        assertTrue(body.contains("\"status\""));
        assertTrue(body.contains("\"UP\""));
        assertTrue(body.contains("\"timestamp\""));
    }

    @Test
    public void healthEndpoint_ShouldReturnJSONContentType() {
        Response response = target("/health")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaderString("Content-Type").startsWith(MediaType.APPLICATION_JSON));
    }
}
```

- [ ] **Step 2: 运行测试验证通过**

```bash
mvn test -pl WebApi
```

预期：`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS。

- [ ] **Step 3: Commit**

```bash
git add WebApi/src/test/java/org/cleancoders/web/HealthResourceTest.java
git commit -m "test: add HealthResource integration test"
```

---

### Task 7: 整体构建验证

**Files:**

- 无新建/修改文件

- [ ] **Step 1: 全量 Maven 构建（编译 + 测试 + 打包）**

```bash
mvn clean package
```

预期：所有模块编译成功，测试全部通过，`WebApi/target/WebApi-1.0-SNAPSHOT.war` 生成。

- [ ] **Step 2: 验证 WAR 包结构**

```bash
jar tf WebApi/target/WebApi-1.0-SNAPSHOT.war | grep -E "WEB-INF/(web\.xml|classes|lib)"
```

预期输出包含：

```
WEB-INF/web.xml
WEB-INF/classes/org/cleancoders/web/AppConfig.class
WEB-INF/classes/org/cleancoders/web/resource/HealthResource.class
WEB-INF/classes/org/cleancoders/web/filter/CorsFilter.class
WEB-INF/lib/jersey-container-servlet-3.1.7.jar
...
```

- [ ] **Step 3: Commit（如有未提交内容）**

```bash
git status
```

---

## 完成标准

- [x] 父 POM 包含 WebApi 模块
- [x] WebApi 模块可独立编译
- [x] `GET /api/health` 返回 `{"status":"UP","timestamp":"..."}`
- [x] CORS 响应头出现在所有响应中
- [x] 集成测试通过
- [x] WAR 包可正常生成