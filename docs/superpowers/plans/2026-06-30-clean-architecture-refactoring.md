# Clean Architecture 重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目重构为 Clean Architecture 骨架：业务模块内部 domain/outbound/usecase 三层 + 新建 Infrastructure 模块 +
WebApi 集成 HK2。

**Architecture:** 功能模块保留为 Maven jar，内部分 domain/outbound/usecase 子包；Infrastructure 模块实现各 outbound
接口；WebApi 通过 HK2 绑定依赖。

**Tech Stack:** Java 17, Jakarta Servlet 6.1, Jersey 3.1.7, HK2, Maven 多模块

## Global Constraints

- Java 17
- 包命名：`org.cleancoders.{module}.{domain|outbound|usecase}.*`
- Infrastructure 包：`org.cleancoders.infrastructure.*`
- domain 层无外部依赖，outbound 仅依赖 domain，usecase 依赖 domain + outbound
- Infrastructure 依赖所有业务模块，WebApi 依赖所有业务模块 + Infrastructure
- Commit message 使用中文
- 不实现任何具体业务逻辑（骨架阶段）

## 文件结构总览

```
（现有模块重构）
UserAndAuth/src/main/java/org/cleancoders/
├── userandauth/domain/          ← 新建
├── userandauth/outbound/        ← 新建
├── userandauth/usecase/         ← 新建
└── Main.java                    ← 删除

SeatAndRoom/ 同上
Reservation/ 同上
SystemTask/  同上

（新建模块）
Infrastructure/
├── pom.xml
└── src/main/java/org/cleancoders/infrastructure/

（WebApi 调整）
WebApi/pom.xml                   ← 新增模块依赖
WebApi/src/main/java/.../web/
├── AppConfig.java               ← 注册 AppBinder
├── binder/
│   └── AppBinder.java           ← 新建：HK2 绑定骨架
├── filter/CorsFilter.java       ← 不变
└── resource/HealthResource.java ← 不变
```

---

### Task 1: 清理业务模块并创建三层包结构

**Files:**

- Delete: `Reservation/src/main/java/org/cleancoders/Main.java`
- Delete: `SeatAndRoom/src/main/java/org/cleancoders/Main.java`
- Delete: `SystemTask/src/main/java/org/cleancoders/Main.java`
- Create 12 个包目录（每模块 3 个：domain/outbound/usecase）

**Interfaces:**

- Produces: 空包结构，供后续 Task 引用

- [ ] **Step 1: 删除所有 Main.java 占位文件**

```bash
rm Reservation/src/main/java/org/cleancoders/Main.java
rm SeatAndRoom/src/main/java/org/cleancoders/Main.java
rm SystemTask/src/main/java/org/cleancoders/Main.java
```

（UserAndAuth 的 Main.java 已在之前的操作中删除）

- [ ] **Step 2: 创建各模块的三层包目录**

```bash
# UserAndAuth
mkdir -p UserAndAuth/src/main/java/org/cleancoders/userandauth/domain
mkdir -p UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound
mkdir -p UserAndAuth/src/main/java/org/cleancoders/userandauth/usecase

# SeatAndRoom
mkdir -p SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain
mkdir -p SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound
mkdir -p SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase

# Reservation
mkdir -p Reservation/src/main/java/org/cleancoders/reservation/domain
mkdir -p Reservation/src/main/java/org/cleancoders/reservation/outbound
mkdir -p Reservation/src/main/java/org/cleancoders/reservation/usecase

# SystemTask
mkdir -p SystemTask/src/main/java/org/cleancoders/systemtask/domain
mkdir -p SystemTask/src/main/java/org/cleancoders/systemtask/outbound
mkdir -p SystemTask/src/main/java/org/cleancoders/systemtask/usecase
```

- [ ] **Step 3: 在每个 domain 包中创建 package-info.java 标注职责**

为每个模块的 domain 包创建 `package-info.java` 说明该层职责（以 UserAndAuth 为例，其他模块同理）：

`UserAndAuth/src/main/java/org/cleancoders/userandauth/domain/package-info.java`:

```java
/**
 * UserAndAuth domain layer.
 * Pure domain entities and value objects.
 * No framework annotations, no dependencies on other project modules.
 */
package org.cleancoders.userandauth.domain;
```

同理为 outbound：
`UserAndAuth/src/main/java/org/cleancoders/userandauth/outbound/package-info.java`:

```java
/**
 * UserAndAuth outbound layer.
 * Interfaces (ports) for external services.
 * Parameters and return values use only domain types or JDK types.
 */
package org.cleancoders.userandauth.outbound;
```

同理为 usecase：
`UserAndAuth/src/main/java/org/cleancoders/userandauth/usecase/package-info.java`:

```java
/**
 * UserAndAuth use case layer.
 * Application business logic orchestrating domain entities and outbound ports.
 * Depends on outbound interfaces via constructor injection, not on infrastructure.
 */
package org.cleancoders.userandauth.usecase;
```

为 SeatAndRoom、Reservation、SystemTask 创建相同的三层 package-info.java（调整模块名注释）。

- [ ] **Step 4: 验证编译**

```bash
mvn compile
```

预期：所有模块 BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: 清理占位文件，创建业务模块 domain/outbound/usecase 包结构"
```

---

### Task 2: 创建 Infrastructure 模块

**Files:**

- Create: `Infrastructure/pom.xml`
- Create: 包目录及 `package-info.java`

**Interfaces:**

- Produces: Infrastructure 模块可供 WebApi 依赖

- [ ] **Step 1: 创建 Infrastructure 目录结构**

```bash
mkdir -p Infrastructure/src/main/java/org/cleancoders/infrastructure
mkdir -p Infrastructure/src/test/java/org/cleancoders/infrastructure
```

- [ ] **Step 2: 创建 Infrastructure/pom.xml**

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

    <artifactId>Infrastructure</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>UserAndAuth</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>SeatAndRoom</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Reservation</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>SystemTask</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 更新父 POM 添加 Infrastructure 模块**

在 `pom.xml` 的 `<modules>` 中添加 `<module>Infrastructure</module>`：

```xml
<modules>
    <module>UserAndAuth</module>
    <module>SeatAndRoom</module>
    <module>Reservation</module>
    <module>SystemTask</module>
    <module>Infrastructure</module>
    <module>WebApi</module>
</modules>
```

- [ ] **Step 4: 创建 package-info.java**

`Infrastructure/src/main/java/org/cleancoders/infrastructure/package-info.java`:

```java
/**
 * Infrastructure layer.
 * Implements outbound interfaces defined in business modules.
 * May introduce external framework dependencies (JDBC, JPA, etc.).
 */
package org.cleancoders.infrastructure;
```

- [ ] **Step 5: 验证编译**

```bash
mvn compile
```

预期：所有模块 BUILD SUCCESS，Infrastructure 编译通过。

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: 新建 Infrastructure 模块，依赖所有业务模块"
```

---

### Task 3: 更新 WebApi 模块依赖和包结构

**Files:**

- Modify: `WebApi/pom.xml`
- Create: `WebApi/src/main/java/org/cleancoders/web/binder/`

**Interfaces:**

- Consumes: 所有业务模块 + Infrastructure 模块
- Produces: WebApi 可引用各模块的 usecase/outbound 类

- [ ] **Step 1: 在 WebApi/pom.xml 中添加模块依赖**

在 `<dependencies>` 开头（`jakarta.servlet-api` 之后）新增：

```xml
        <!-- Business modules -->
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>UserAndAuth</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>SeatAndRoom</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Reservation</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>SystemTask</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Infrastructure</artifactId>
            <version>${project.version}</version>
        </dependency>
```

- [ ] **Step 2: 创建 binder 包目录**

```bash
mkdir -p WebApi/src/main/java/org/cleancoders/web/binder
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile -pl WebApi
```

预期：WebApi BUILD SUCCESS，依赖全部解析。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: WebApi 添加业务模块和 Infrastructure 依赖"
```

---

### Task 4: 创建 HK2 AppBinder 骨架

**Files:**

- Create: `WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java`

**Interfaces:**

- Produces: `AppBinder extends AbstractBinder`，供 AppConfig 注册

- [ ] **Step 1: 创建 AppBinder.java**

```java
package org.cleancoders.web.binder;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * HK2 dependency injection binder.
 * Binds outbound interface implementations from Infrastructure module
 * to their corresponding interfaces defined in business modules.
 *
 * Binding rules:
 * - bind(Implementation.class).to(Interface.class);    — create new instance per injection
 * - bind(Implementation.class).to(Interface.class).in(Singleton.class); — singleton
 */
public class AppBinder extends AbstractBinder {

    @Override
    protected void configure() {
        // === UserAndAuth ===
        // bind(InMemoryUserRepo.class).to(UserRepository.class);

        // === SeatAndRoom ===
        // bind(InMemorySeatRepo.class).to(SeatRepository.class);

        // === Reservation ===
        // bind(InMemoryReservationRepo.class).to(ReservationRepository.class);

        // === SystemTask ===
        // bind(InMemoryTaskRepo.class).to(TaskRepository.class);
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl WebApi
```

预期：BUILD SUCCESS（HK2 由 jersey-hk2 提供，已在 classpath 中）。

- [ ] **Step 3: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/binder/AppBinder.java
git commit -m "feat: 创建 HK2 AppBinder 绑定骨架"
```

---

### Task 5: 在 AppConfig 中注册 AppBinder

**Files:**

- Modify: `WebApi/src/main/java/org/cleancoders/web/AppConfig.java`

**Interfaces:**

- Consumes: `AppBinder`
- Produces: Jersey 启动时自动执行 HK2 绑定

- [ ] **Step 1: 修改 AppConfig.java，注册 AppBinder**

当前 `AppConfig.java`:

```java
package org.cleancoders.web;

import jakarta.ws.rs.core.Application;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.resource.HealthResource;

import java.util.HashSet;
import java.util.Set;

public class AppConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(HealthResource.class);
        classes.add(CorsFilter.class);
        return classes;
    }
}
```

修改后：

```java
package org.cleancoders.web;

import jakarta.ws.rs.core.Application;
import org.cleancoders.web.binder.WebAppBinder;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.resource.HealthResource;

import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application configuration.
 * Registers all resource classes, providers, and HK2 binders.
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
        // HK2 Binder (must be registered as a class so Jersey discovers it)
        classes.add(AppBinder.class);
        return classes;
    }
}
```

- [ ] **Step 2: 运行测试验证**

```bash
mvn test -pl WebApi
```

预期：2/2 tests pass，HK2 绑定不干扰现有测试。

- [ ] **Step 3: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/AppConfig.java
git commit -m "feat: AppConfig 注册 HK2 AppBinder"
```

---

### Task 6: 全量构建验证

**Files:** 无新建

- [ ] **Step 1: 全量编译 + 测试 + 打包**

```bash
mvn clean package
```

预期：

- 所有模块 BUILD SUCCESS
- WebApi 测试 2/2 pass
- `WebApi/target/WebApi-1.0-SNAPSHOT.war` 生成
- WAR 中包含 Infrastructure jar

- [ ] **Step 2: 验证 WAR 包含 Infrastructure 模块**

```bash
jar tf WebApi/target/WebApi-1.0-SNAPSHOT.war | grep "Infrastructure"
```

预期：输出 `WEB-INF/lib/Infrastructure-1.0-SNAPSHOT.jar`

- [ ] **Step 3: 验证模块间依赖隔离**

```bash
# domain 层不应依赖其他模块（检查 UserAndAuth domain 的 classpath）
mvn dependency:tree -pl UserAndAuth 2>&1 | grep -v "test"
```

预期：UserAndAuth 不依赖 Infrastructure 或 WebApi。

- [ ] **Step 4: Commit（如有未提交内容）**

```bash
git status
```

---

## 完成标准

- [x] 4 个业务模块均有 domain/outbound/usecase 三层包结构
- [x] Infrastructure 模块存在且依赖所有业务模块
- [x] WebApi 依赖所有业务模块 + Infrastructure
- [x] HK2 AppBinder 骨架就绪，AppConfig 已注册
- [x] `mvn clean package` 全量通过
- [x] WAR 包含全部模块 jar
- [x] 不包含任何具体业务逻辑实现