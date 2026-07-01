# Clean Architecture 项目重构设计方案

## 概述

将座位预订系统项目按 Clean Architecture / SOLID 原则重构，垂直拆分为：

- **WebApi**（展示层）— JAX-RS Resource + HK2 依赖绑定
- **业务模块内部三层** — domain / outbound / usecase
- **Infrastructure** — 实现 OutBound 接口

每个功能模块保留为独立 Maven 模块，内部按层拆分。

## 模块结构

```
SeatReservationSystem (pom 父模块)
├── WebApi (war)
├── UserAndAuth (jar)
│   ├── domain/...          ← 实体、值对象
│   ├── outbound/...        ← 接口（Port）
│   └── usecase/...         ← 业务编排
├── SeatAndRoom (jar)       ← 同上
├── Reservation (jar)       ← 同上
├── SystemTask (jar)        ← 同上
└── Infrastructure (jar)    ← 实现各模块的 OutBound 接口
```

## 依赖关系

```
WebApi ──→ usecase（各模块） + Infrastructure（仅 HK2 绑定）
Infrastructure ──→ outbound（各模块接口） + domain（各模块实体）

各业务模块内部：
  domain    → 无外部依赖
  outbound  → 依赖 domain
  usecase   → 依赖 domain + outbound
```

## 各层职责

| 层                  | 包路径格式                              | 规则                                        |
|--------------------|------------------------------------|-------------------------------------------|
| **domain**         | `{模块包}.domain.*`                   | 纯 POJO，无框架注解，不依赖项目内其他模块                   |
| **outbound**       | `{模块包}.outbound.*`                 | 只含接口（Port），参数/返回值只用 domain 类型或 JDK 类型     |
| **usecase**        | `{模块包}.usecase.*`                  | 依赖 outbound 接口（构造函数注入），不依赖 Infrastructure |
| **Infrastructure** | `org.cleancoders.infrastructure.*` | 实现各模块的 outbound 接口，可引入外部框架依赖              |
| **WebApi**         | `org.cleancoders.web.resource.*`   | 依赖 usecase，负责 HTTP 请求/响应转换                |

## 包命名规范

- WebApi: `org.cleancoders.web.*`
- UserAndAuth: `org.cleancoders.userandauth.{domain,outbound,usecase}.*`
- SeatAndRoom: `org.cleancoders.seatandroom.{domain,outbound,usecase}.*`
- Reservation: `org.cleancoders.reservation.{domain,outbound,usecase}.*`
- SystemTask: `org.cleancoders.systemtask.{domain,outbound,usecase}.*`
- Infrastructure: `org.cleancoders.infrastructure.{userandauth,seatandroom,...}.*`

## 功能模块内部示例（UserAndAuth）

```
UserAndAuth/src/main/java/org/cleancoders/userandauth/
├── domain/
│   ├── User.java              ← 实体
│   └── UserRole.java          ← 值对象 / 枚举
├── outbound/
│   └── UserRepository.java    ← 接口：findById, save, delete ...
└── usecase/
    └── RegisterUseCase.java   ← 构造函数注入 UserRepository，编排注册逻辑
```

## 依赖注入（HK2）

在 WebApi 模块中创建 HK2 Binder，绑定接口到实现：

```java
public class AppBinder extends AbstractBinder {
    @Override
    protected void configure() {
        // UserAndAuth
        bind(JdbcUserRepo.class).to(UserRepository.class);
        // SeatAndRoom
        bind(JdbcSeatRepo.class).to(SeatRepository.class);
        // ...
    }
}
```

AppBinder 在 AppConfig 中注册。

## 阶段划分

### 阶段 1（本次）：架构骨架搭建

- 重建各业务模块的 domain/outbound/usecase 包结构
- 新建 Infrastructure 模块
- 清理各模块无用文件（Main.java 等）
- 调整 WebApi 模块依赖和包结构
- 创建 HK2 Binder 骨架

### 阶段 2（后续）：业务功能实现

- 逐模块实现 domain 实体、outbound 接口、usecase、infrastructure 实现

## 技术栈

- Java 17
- Jakarta Servlet 6.1 + Jersey 3.1.7 (JAX-RS)
- Jackson (JSON)
- HK2 (DI)
- Maven 多模块