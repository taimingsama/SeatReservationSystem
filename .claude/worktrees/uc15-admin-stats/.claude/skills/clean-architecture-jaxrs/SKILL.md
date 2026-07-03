---
name: clean-architecture-jaxrs
description: >
  Use this skill whenever the user asks to add a new feature, use case, endpoint, or domain entity to this project.
  It describes the Clean Architecture + JAX-RS patterns used in this codebase: UseCase inheritance hierarchy
  (Template Method with AuthUseCase/StudentAuthUseCase/AdminAuthUseCase), Presenter + ThreadLocal pattern,
  AppBinder dependency injection binding, JWT authentication flow, and module responsibility boundaries.
  Trigger on: "add a feature", "create a new endpoint", "add a use case", "new API", "implement UC-XX",
  "add a domain entity", "create a resource", "wire up a presenter", or any mention of the project's
  architecture layers (usecase, outbound, webapi, domain, infrastructure).
---

# Clean Architecture + JAX-RS 项目架构指南

## 架构概述

本项目采用 **Clean Architecture** 分层架构，结合 **JAX-RS (Jersey)** 作为 Web 层，
使用 **Presenter 模式** 解耦业务逻辑与 HTTP 响应格式，以及 **Template Method 模式**
实现 JWT 认证和角色权限控制。

## 分层结构

```
┌──────────────────────────────────────────────────┐
│  WebApi 层 (JAX-RS Resource / Presenter / Filter) │  ← HTTP 协议适配
├──────────────────────────────────────────────────┤
│  UseCase 层 (业务用例编排，认证 + 授权)             │  ← 应用逻辑
├──────────────────────────────────────────────────┤
│  Domain 层 (实体 / 值对象)                         │  ← 领域模型
├──────────────────────────────────────────────────┤
│  Outbound 层 (接口契约 — Repository, Service)      │  ← 抽象网关
├──────────────────────────────────────────────────┤
│  Infrastructure 层 (JDBC / InMemory / JWT / BCrypt)│  ← 技术实现
└──────────────────────────────────────────────────┘
```

**依赖方向**（Clean Architecture 核心规则）:

```
WebApi ──→ UseCase ──→ Outbound (接口) ←── Infrastructure
                │            ↑                   │
                └──→ Domain (实体) ←──────────────┘
```

- UseCase 层 **只依赖** Domain 实体和 Outbound 接口——不依赖 Infrastructure 具体实现
- Infrastructure 层 **实现** Outbound 接口——依赖反转，UseCase 不知道实现细节
- Domain 层是最内层，不依赖任何外部

## 模块划分

按业务子域划分为 4 个模块，每个模块内部再按层组织。
**Domain + Outbound 在模块内**（与模块业务绑定），**Infrastructure + WebApi 跨模块**（技术实现与协议适配）。

| 模块              | 职责       | domain 包                                      | outbound 包 (接口)                                          | usecase 包                                                                                                                                   |
|-----------------|----------|-----------------------------------------------|----------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| **UserAndAuth** | 用户、认证、授权 | (见 Common)                                    | `UserRepository`, `TokenService`, `PasswordEncoder`      | `RegisterUseCase`, `LoginUseCase`, `GetMeUseCase`, `AuthUseCase`(abstract), `StudentAuthUseCase`, `AdminAuthUseCase`                        |
| **SeatAndRoom** | 自习室、座位管理 | `StudyRoom`, `Seat`, `SeatStatus`, `TimeSlot` | `RoomRepository`, `SeatRepository`, `TimeSlotRepository` | `ListRoomsUseCase`, `ListSeatsUseCase`, `ManageRoomsUseCase`, `ManageSeatsUseCase`                                                          |
| **Reservation** | 预约、签到、退座 | `Reservation`, `ReservationStatus`            | `ReservationRepository`                                  | `ReserveUseCase`, `CheckInUseCase`, `CheckOutUseCase`, `CancelReservationUseCase`, `ListMyReservationsUseCase`, `ManageReservationsUseCase` |
| **SystemTask**  | 定时任务、统计  | `Stats` (value object)                        | `StatsRepository`                                        | `AutoReleaseUseCase`, `GetStatsUseCase`                                                                                                     |

**Infrastructure 层**（跨模块，实现 Outbound 接口）: `InMemoryUserRepo` → `UserRepository`, `JjwtTokenService` →
`TokenService`, `BCryptPasswordEncoder` → `PasswordEncoder`, `AutoReleaseScheduler`

**WebApi 层**（跨模块）:

- `resource`: `AuthResource`, `RoomResource`, `SeatResource`, `ReservationResource`, `AdminResource`, `StatsResource`
- `filter`: `CorsFilter`, `AuthFilter`
- `presenter`: `WebApiXxxPresenter`（实现各 UseCase 内嵌的 Presenter 接口）
- `binder`: `AppBinder`

## 公共模块提取规则

当多个模块共用同一个类（实体 / 接口 / 枚举），**必须**将其提取到独立的公共模块中，避免模块间直接依赖导致的循环引用和耦合。

### 命名规则

| 共享范围           | 模块名                       | 说明                                           |
|----------------|---------------------------|----------------------------------------------|
| **所有模块** 都用到   | `Common`                  | 纯公共模块，不依赖任何业务模块                              |
| **两个模块** 共用    | `Common{模块A}_{模块B}`       | 例：`CommonSeatAndRoom_Reservation`            |
| **三个及以上** 模块共用 | `Common{模块A}_{模块B}_{模块C}` | 例：`CommonUserAndAuth_Reservation_SystemTask` |

关键约束：

- 分隔符用 **`_`**（下划线），不是 `And`
- 模块名用简称（`UserAndAuth`、`SeatAndRoom`、`Reservation`、`SystemTask`）
- Common 模块之间可以互相依赖（如 `CommonSeatAndRoom_Reservation` 依赖 `Common`）

### 已创建的公共模块

| 模块           | 包含                 | 使用者                                          |
|--------------|--------------------|----------------------------------------------|
| **`Common`** | `User`, `UserRole` | UserAndAuth, Infrastructure, WebApi（及未来所有模块） |

### 规划中的公共模块

| 模块                                  | 包含                                                               | 使用者                      |
|-------------------------------------|------------------------------------------------------------------|--------------------------|
| **`CommonSeatAndRoom_Reservation`** | `Seat`, `SeatStatus`, `StudyRoom`, `TimeSlot` + 对应 Repository 接口 | SeatAndRoom, Reservation |
| **`CommonReservation_SystemTask`**  | `Reservation`, `ReservationStatus` + `ReservationRepository`     | Reservation, SystemTask  |

### 判断流程

新增实体或接口时，按以下决策树选择放置位置：

```
新增的类被哪几个模块使用？
  ├── 被所有模块使用？
  │     └── 放入 Common
  ├── 被 2 个模块使用？
  │     └── 创建 Common{A}_{B}，放入其中
  ├── 被 3 个及以上模块使用？
  │     └── 创建 Common{A}_{B}_{C}，放入其中
  └── 只被 1 个模块使用？
        └── 放在该模块自己的 domain/outbound 包中
```

### 公共模块结构

公共模块与业务模块结构一致，按层组织：

```
CommonSeatAndRoom_Reservation/
├── pom.xml                          ← 依赖 Common
└── src/main/java/org/cleancoders/
    └── commonseatandroom_reservation/
        ├── domain/                   ← 共享实体（Seat, StudyRoom, TimeSlot）
        └── outbound/                 ← 共享仓储接口（SeatRepository 等）
```

> **注意**：公共模块只放 domain 实体和 outbound 接口，**不放** UseCase。UseCase 始终属于其业务模块。

### 为什么需要公共模块

如果 Reservation 直接依赖 SeatAndRoom 来获取 `Seat` 类：

- Reservation 会被迫引入 SeatAndRoom 的所有传递依赖
- 容易形成循环依赖（如 SeatAndRoom 后来又需要 Reservation 的类型）
- 单元测试时必须加载整个 SeatAndRoom 模块

提取到轻量的 Common 模块后，依赖方只需引入一个只含共享类型的 jar。

## 核心模式一：UseCase 继承体系 (Template Method)

### 继承树

```
AuthUseCase (abstract)
  ├── 持有 TokenService + UserRepository
  ├── execute(request) {
  │     user = authenticate(request.token())
  │     authorize(user, request)     ← hook，子类覆盖
  │     doExecute(user, request)     ← abstract，子类实现
  │   }
  │
  ├── StudentAuthUseCase (abstract)
  │     authorize: 检查 user.role == STUDENT，否则 403
  │     ├── ReserveUseCase
  │     ├── CheckInUseCase
  │     ├── CheckOutUseCase
  │     ├── CancelReservationUseCase
  │     └── ListMyReservationsUseCase
  │
  ├── AdminAuthUseCase (abstract)
  │     authorize: 检查 user.role == ADMIN，否则 403
  │     ├── ManageRoomsUseCase
  │     ├── ManageSeatsUseCase
  │     ├── ManageReservationsUseCase
  │     └── GetStatsUseCase
  │
  └── 公开用例（不继承 AuthUseCase，无认证要求）
        RegisterUseCase, LoginUseCase, GetMeUseCase
        ListRoomsUseCase, ListSeatsUseCase
```

### 创建新用例时的决策

当新增功能时，按以下规则选择基类：

1. **仅登录用户可访问** → 继承 `AuthUseCase<R>`
2. **仅学生可访问** → 继承 `StudentAuthUseCase<R>`
3. **仅管理员可访问** → 继承 `AdminAuthUseCase<R>`
4. **公开访问（无需登录）** → 不继承任何 Auth 基类，直接实现独立的 execute 方法

### 为什么这样设计

Template Method 将认证和授权逻辑集中在基类中，避免每个 UseCase 重复编写 token 解析和角色检查代码。子类只需关注 `doExecute`
中的纯业务逻辑。当需要新增角色时，只需新增一个 AuthUseCase 子类即可。

## 核心模式：Domain 层（富领域模型）

Domain 不仅是 data holder（record/POJO），更应该包含 **不依赖 Outbound 接口** 的纯领域业务逻辑。
这些逻辑直接内聚在实体中，不需要任何外部依赖即可单测。

### 什么应该放在 Domain 实体中

- **状态转换**：`reservation.cancel()` 内部校验当前状态是否允许取消，然后修改状态
- **业务规则判断**：`reservation.isExpired(now)` 判断是否超时
- **不变量校验**：`seat.reserve()` 内部检查 `status == AVAILABLE` 才允许
- **值计算**：`timeSlot.duration()` 返回时段长度

### 范例

```java
// ❌ 贫血模型 — 业务逻辑散落在 UseCase 中
public class Reservation extends ... {
    // UseCase 中写: if (r.getStatus() == RESERVED) { r.setStatus(CANCELLED); }
}

// ✅ 富领域模型 — 业务逻辑内聚在实体中
public class Reservation {
    private ReservationStatus status;

    public void cancel() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("只有已预约状态才能取消");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void checkIn() { ... }
    public boolean isExpired(LocalDateTime now) { ... }
}
```

### 什么不应该放在 Domain 中

- 需要查数据库的逻辑 → 放在 UseCase（通过注入 Repository）
- 需要调用外部服务的逻辑 → 放在 UseCase（通过注入 Outbound 接口）
- HTTP 协议相关 → 放在 WebApi 层

## 核心模式二：Outbound 接口层（依赖反转）

### 概念

Outbound 层是 **接口契约层**，定义 UseCase 需要但由 Infrastructure 实现的能力。
它位于模块内部（`{module}.outbound` 包），是 UseCase 和 Infrastructure 之间的桥梁。

**核心规则**：UseCase **只依赖** Outbound 接口，**不依赖** Infrastructure 具体实现。

```
UseCase ──注入──→ Outbound 接口 ←──实现── Infrastructure
  (业务逻辑)       (抽象契约)           (技术细节)
```

### 三种 Outbound 接口类型

| 类型             | 命名模式             | 示例                                        | 职责             |
|----------------|------------------|-------------------------------------------|----------------|
| **Repository** | `{实体}Repository` | `UserRepository`, `ReservationRepository` | 持久化（CRUD、查询）   |
| **Service**    | `{能力}Service`    | `TokenService`, `PasswordEncoder`         | 基础设施服务（加密、JWT） |
| **调度器**        | `{任务}Scheduler`  | `AutoReleaseScheduler`                    | 定时任务触发         |

### 现有 Outbound 接口

```java
// UserAndAuth 模块 — 用户持久化
public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);
    User save(User user);
}

// UserAndAuth 模块 — JWT Token 生成
public interface TokenService {
    String generate(String userId, String username, String role);
}

// UserAndAuth 模块 — 密码加密
public interface PasswordEncoder {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
```

### Infrastructure 如何实现 Outbound 接口

```
Outbound 接口 (在模块内)          Infrastructure 实现 (跨模块)
─────────────────────────        ──────────────────────────
UserRepository          ←────    InMemoryUserRepo
TokenService            ←────    JjwtTokenService
PasswordEncoder         ←────    BCryptPasswordEncoder
```

- 命名：`{技术前缀}{接口名}` — `InMemoryUserRepo`, `JjwtTokenService`, `BCryptPasswordEncoder`
- 用 `@Singleton` 标注（无状态）
- 在 `AppBinder` 中绑定：`bind(实现.class).to(接口.class).in(Singleton.class)`

### 为什么不直接注入 Infrastructure

如果 UseCase 直接注入 `InMemoryUserRepo` 而不是 `UserRepository`：

- 切换存储实现（如 InMemory → JDBC）需要改 UseCase 代码
- 单元测试无法 Mock——必须用真实数据库

通过 Outbound 接口，UseCase 只看到契约，测试时注入 Mock 即可，生产环境切换实现只需改 AppBinder。

### 新增 Outbound 接口的规则

1. 接口定义在对应模块的 `outbound` 包下
2. 接口方法只表达"做什么"，不暴露技术细节（如不暴露 `Connection`、`Sql`）
3. 参数和返回值使用 Domain 实体或基本类型，不引入 Infrastructure 类型
4. Infrastructure 实现类放在 `infrastructure` 包下，类名加技术前缀

## 核心模式三：UseCase 内嵌 Input / Output / Presenter

每个 UseCase 类内部定义三个接口类型，形成自包含的契约：

```java
public class ReserveUseCase extends StudentAuthUseCase<ReserveUseCase.Request> {

    // === 构造注入的依赖 ===
    @Inject protected Presenter presenter;         // ← Presenter，注入具体实现
    @Inject protected ReservationRepository reservationRepo;
    @Inject protected SeatRepository seatRepo;

    // === 内嵌 Presenter 接口 ===
    // 定义所有可能的输出分支（成功 + 各类业务错误）
    public interface Presenter {
        void success(String reservationId, String seatNumber, String timeSlot);
        void seatNotAvailable(String seatId, String timeSlot);
        void duplicateReservation(String existingId);
    }

    // === 内嵌 Output（execute 的返回值）===
    // record 类型，精简，仅含业务标识字段
    public record Output(String reservationId) {}

    // === 内嵌 Request（实现 AuthRequest 接口）===
    // record 类型，自动获得 equals/hashCode/toString
    public record Request(String token, String seatId, String timeSlotId, LocalDate date)
        implements AuthRequest {}

    // === 业务逻辑 ===
    @Override
    protected Output doExecute(User user, Request req) {
        // 1. 冲突检测
        // 2. 持久化
        Reservation r = reservationRepo.save(...)
        // 3. 通知 Presenter（设置 HTTP 响应）
        presenter.success(r.getId(), r.getSeatNumber(), r.getTimeSlotLabel());
        // 4. 返回 Output（供 Resource 层获取业务标识）
        return new Output(r.getId());
    }
}
```

### 参数数量原则

- **Request**: 参数容纳在 record 中，数量不限
- **Presenter 方法**: 参数尽量少（≤4 个），只传入展示层真正需要的数据
- **Output**: 字段尽量精简，仅含业务标识（如 id）

### 为什么需要内嵌接口

将 Presenter 接口定义在 UseCase 内部，让 UseCase 成为"自说明"的契约：一眼就能看到这个用例有哪些输出分支。WebApi
层实现这些接口时，IDE 会强制覆盖所有方法，不会遗漏错误处理分支。

## 核心模式四：Presenter + ThreadLocal

WebApi 层的 Presenter 实现使用 `ThreadLocal<Response>` 存储当前请求的 HTTP 响应：

```java
@Singleton  // ← 单例，所有请求共享同一个实例
public class WebApiReservationPresenter implements ReserveUseCase.Presenter {
    private final ThreadLocal<Response> current = new ThreadLocal<>();

    @Override
    public void success(String reservationId, String seatNumber, String timeSlot) {
        current.set(Response.status(201).entity(Map.of(
            "reservationId", reservationId,
            "seatNumber", seatNumber,
            "timeSlot", timeSlot
        )).build());
    }

    @Override
    public void seatNotAvailable(String seatId, String timeSlot) {
        current.set(Response.status(409).entity(Map.of(
            "error", "座位已被预约",
            "seatId", seatId,
            "timeSlot", timeSlot
        )).build());
    }

    @Override
    public void duplicateReservation(String existingId) {
        current.set(Response.status(409).entity(Map.of(
            "error", "该时段已有预约",
            "existingReservationId", existingId
        )).build());
    }

    public Response getResponse() {
        return current.get();
    }
}
```

### 为什么用 ThreadLocal

Presenter 是 `@Singleton` 实例（DI 容器中只有一个），但每个 HTTP 请求运行在独立线程中。ThreadLocal 确保每个请求写入自己的
Response，互不干扰。

### HTTP 状态码约定

| 场景                 | 状态码 |
|--------------------|-----|
| 创建成功               | 201 |
| 查询成功               | 200 |
| 业务冲突（如座位已被预约、重复预约） | 409 |
| 权限不足               | 403 |
| 资源不存在              | 404 |
| 参数校验失败             | 400 |

## 核心模式五：Resource 层的调用模式

Resource 是 JAX-RS 端点，负责提取 HTTP 参数、构建 Request、调用 UseCase、获取 Response：

```java
@Path("/reservations")
public class ReservationResource {
    @Inject private ReserveUseCase reserveUseCase;
    @Inject private WebApiReservationPresenter presenter;  // ← 注入 Presenter 实例

    @POST
    public Response reserve(@HeaderParam("Authorization") String auth, ReserveInput input) {
        ReserveUseCase.Output output = reserveUseCase.execute(
            new ReserveUseCase.Request(
                extractBearer(auth),       // ← 去掉 "Bearer " 前缀
                input.seatId,
                input.timeSlotId,
                input.date
            )
        );
        // output.reservationId 可用于日志、后续操作等
        return presenter.getResponse();   // ← 从 ThreadLocal 取 Response
    }
}
```

### 执行流程

```
客户端 → AuthFilter（仅提取 Bearer Token，不验证）
       → Resource（从 Header 取 token，构建 Request 传给 UseCase）
       → UseCase.authenticate(token)
            → TokenService.validate(token)          ← JWT 解析 + 签名验证（Infrastructure）
            → UserRepository.findById(userId)       ← 确认用户存在
       → authorize(user, request)                ← 角色检查（子类覆盖）
       → doExecute(user, request)                ← 业务逻辑
       → presenter.success(output)               ← 写入 Response 到 ThreadLocal
       → presenter.getResponse()                 ← Resource 取回 Response
```

### Token 提取

AuthFilter **仅提取** Bearer Token，不验证签名——验证交给 UseCase 层的 `TokenService`。

## 核心模式六：AppBinder 依赖注入绑定

```java
public class AppBinder extends AbstractBinder {
    @Override
    protected void configure() {
        // === UseCase：PerLookup（每次注入创建新实例）===
        bind(ReserveUseCase.class).to(ReserveUseCase.class);
        bind(CheckInUseCase.class).to(CheckInUseCase.class);

        // === Presenter：Singleton（单例，ThreadLocal 保证线程隔离）===
        bind(WebApiReservationPresenter.class)
            .to(ReserveUseCase.Presenter.class)   // ← 绑定到 UseCase 内嵌接口
            .in(Singleton.class);

        // === Outbound 接口 → Infrastructure 实现 ===
        bind(InMemoryUserRepo.class)
            .to(UserRepository.class)
            .in(Singleton.class);
        bind(JjwtTokenService.class)
            .to(TokenService.class)
            .in(Singleton.class);
        bind(BCryptPasswordEncoder.class)
            .to(PasswordEncoder.class)
            .in(Singleton.class);
    }
}
```

### 绑定规则

| 组件类型                           | 作用域               | 原因                      |
|--------------------------------|-------------------|-------------------------|
| UseCase                        | **PerLookup**（默认） | 每次请求新建实例，可持有请求级状态       |
| Presenter                      | **Singleton**     | 单例 + ThreadLocal 实现线程安全 |
| Repository                     | **Singleton**     | JDBC 实现无状态              |
| TokenService / PasswordEncoder | **Singleton**     | 无状态                     |

## 新增功能的完整步骤

当新增一个功能时，按照以下步骤操作。以 "评价座位" 功能（Reservation 模块，学生权限）为例。

### 决策优先：哪些需要新建，哪些复用

新增功能前，先判断：

1. **需要新实体吗？** → 需要新数据表/新业务概念 → 新建 Domain 实体
2. **需要新的 Outbound 接口吗？** → 需要新的数据访问/外部服务 → 新建 Repository/Service 接口
3. **已有的 Outbound 接口够用吗？** → 复用已有接口，只加方法
4. **需要新的权限级别吗？** → 如果没有现成基类，新增 AuthUseCase 子类

### 文件清单

```
reservation/domain/
  └── Review.java                    ← 实体（含业务逻辑，不只是 data holder）
reservation/outbound/
  └── ReviewRepository.java          ← 接口契约（UseCase 只依赖这个，不依赖实现）
reservation/usecase/
  └── ReviewSeatUseCase.java         ← UseCase（继承 StudentAuthUseCase，注入 Outbound 接口）
infrastructure/persistence/
  └── InMemoryReviewRepo.java        ← 实现 ReviewRepository 接口
webapi/
  ├── presenter/
  │   └── WebApiReviewPresenter.java ← 实现 ReviewSeatUseCase.Presenter
  ├── resource/
  │   └── ReviewResource.java        ← JAX-RS Resource
  └── binder/
      └── AppBinder.java             ← 追加绑定（修改已有文件）
```

### 步骤 1：Domain 层 — 实体 + 业务逻辑

在对应模块的 `domain` 包下创建实体。**不只是 record**——把不依赖 Outbound 的业务规则内聚进来：

```java
package reservation.domain;

public class Review {
    private Long id;
    private Long userId;
    private Long seatId;
    private int rating;
    private String comment;

    // 构造器 ...

    // === 领域业务逻辑：不依赖任何 Outbound 接口 ===
    public boolean isValidRating() {
        return rating >= 1 && rating <= 5;
    }

    public void fixRating() {
        if (rating < 1) this.rating = 1;
        if (rating > 5) this.rating = 5;
    }

    // getters ...
}
```

### 步骤 2：Outbound 层 — 接口契约

在对应模块的 `outbound` 包下创建接口。**只定义"做什么"，不暴露技术细节**：

```java
package reservation.outbound;

public interface ReviewRepository {
    void save(Review review);
    List<Review> findBySeatId(Long seatId);
    boolean existsByUserIdAndSeatId(Long userId, Long seatId);
}
```

关键约束：

- 参数和返回值用 Domain 类型或基本类型——不引入 `Connection`、`ResultSet`、`HttpClient` 等技术类型
- 接口在模块内（`reservation.outbound`），实现在跨模块的 `infrastructure` 中

### 步骤 3：UseCase 层 — 注入 Outbound 接口

```java
package reservation.usecase;

public class ReviewSeatUseCase extends StudentAuthUseCase<ReviewSeatUseCase.Request> {

    // 注入的都是 Outbound 接口，不是具体实现
    @Inject protected Presenter presenter;
    @Inject protected ReviewRepository reviewRepo;          // ← 接口
    @Inject protected ReservationRepository reservationRepo; // ← 接口

    public interface Presenter {
        void success(Long reviewId);
        void seatNotReserved();
        void alreadyReviewed();
        void invalidRating();                               // ← 新分支：评分不合法
    }

    public record Output(Long reviewId) {}

    public record Request(String token, Long seatId, int rating, String comment)
        implements AuthRequest {}

    @Override
    protected Output doExecute(User user, Request req) {
        // 1. 用 Domain 实体自身的业务逻辑
        Review review = new Review(null, Long.valueOf(user.id()), req.seatId, req.rating, req.comment);
        if (!review.isValidRating()) {
            presenter.invalidRating();
            return new Output(null);
        }

        // 2. 通过 Outbound 接口查数据（不关心是 InMemory 还是 JDBC）
        if (!reservationRepo.existsByUserIdAndSeatId(...)) {
            presenter.seatNotReserved();
            return new Output(null);
        }
        if (reviewRepo.existsByUserIdAndSeatId(...)) {
            presenter.alreadyReviewed();
            return new Output(null);
        }

        // 3. 通过 Outbound 接口持久化
        reviewRepo.save(review);
        presenter.success(review.getId());
        return new Output(review.getId());
    }
}
```

### 步骤 4：Infrastructure 层 — 实现 Outbound 接口

```java
package infrastructure.persistence;

@Singleton
public class InMemoryReviewRepo implements ReviewRepository {
    private final Map<Long, Review> store = new ConcurrentHashMap<>();
    // 实现 ReviewRepository 的所有方法
}
```

命名：`{技术前缀}{接口名}` — `InMemoryReviewRepo`、`JdbcReviewRepo`、`JjwtTokenService`

### 步骤 5：WebApi - Presenter

```java
package webapi.presenter;

@Singleton
public class WebApiReviewPresenter implements ReviewSeatUseCase.Presenter {
    private final ThreadLocal<Response> current = new ThreadLocal<>();

    @Override
    public void success(Long reviewId) {
        current.set(Response.status(201).entity(Map.of("reviewId", reviewId)).build());
    }

    @Override
    public void seatNotReserved() {
        current.set(Response.status(409).entity(Map.of("error", "未预约该座位，无法评价")).build());
    }

    @Override
    public void alreadyReviewed() {
        current.set(Response.status(409).entity(Map.of("error", "已评价过该座位")).build());
    }

    @Override
    public void invalidRating() {
        current.set(Response.status(400).entity(Map.of("error", "评分必须在 1-5 之间")).build());
    }

    public Response getResponse() { return current.get(); }
}
```

### 步骤 6：WebApi - Resource

```java
package webapi.resource;

@Path("/reviews")
public class ReviewResource {
    @Inject private ReviewSeatUseCase reviewSeatUseCase;
    @Inject private WebApiReviewPresenter presenter;

    @POST
    public Response review(@HeaderParam("Authorization") String auth, ReviewInput input) {
        reviewSeatUseCase.execute(
            new ReviewSeatUseCase.Request(extractBearer(auth), input.seatId, input.rating, input.comment)
        );
        return presenter.getResponse();
    }
}
```

### 步骤 7：AppBinder 注册 — 绑定接口到实现

在 `AppBinder.configure()` 中追加。**关键**：绑定 Outbound 接口到 Infrastructure 实现：

```java
// === UseCase ===
bind(ReviewSeatUseCase.class).to(ReviewSeatUseCase.class);

// === Presenter：接口 → WebApi 实现 ===
bind(WebApiReviewPresenter.class)
    .to(ReviewSeatUseCase.Presenter.class)
    .in(Singleton.class);

// === Outbound 接口 → Infrastructure 实现（依赖反转的核心）===
bind(InMemoryReviewRepo.class)
    .to(ReviewRepository.class)        // ← ReviewRepository 是 Outbound 接口
    .in(Singleton.class);
```

### 小结：依赖链

```
ReviewResource
  └─→ ReviewSeatUseCase
        ├─→ ReviewRepository (Outbound 接口) ←── InMemoryReviewRepo (Infrastructure)
        ├─→ ReservationRepository (Outbound 接口) ←── InMemoryReservationRepo (Infrastructure)
        ├─→ Presenter (UseCase 内嵌接口) ←── WebApiReviewPresenter (WebApi)
        └─→ Review (Domain 实体，含 isValidRating 等业务方法)
```

## 领域实体关系

```
StudyRoom  1 ──── N  Seat
Seat       1 ──── N  Reservation
User       1 ──── N  Reservation
TimeSlot   1 ──── N  Reservation
```

### 座位状态流转

```
AVAILABLE ──预约──→ RESERVED ──签到──→ OCCUPIED ──退座──→ AVAILABLE
                     │                      │
                     ├─超时──→ AVAILABLE    │
                     └─取消──→ AVAILABLE    │
                                            
MAINTENANCE ──恢复──→ AVAILABLE
AVAILABLE   ──维护──→ MAINTENANCE
```

## 命名规范

| 元素               | 命名规则                             | 示例                                                              |
|------------------|----------------------------------|-----------------------------------------------------------------|
| UseCase 类        | `{动作}{对象}UseCase`                | `ReserveUseCase`, `ListRoomsUseCase`                            |
| Presenter 接口     | UseCase 内部命名为 `Presenter`        | `ReserveUseCase.Presenter`                                      |
| Presenter 实现     | `WebApi{模块}Presenter`            | `WebApiReservationPresenter`                                    |
| Resource 类       | `{模块}Resource`                   | `ReservationResource`                                           |
| Repository 接口    | `{实体}Repository`                 | `ReservationRepository`                                         |
| Repository 实现    | `{技术前缀}{接口名}`                    | `InMemoryUserRepo`, `JjwtTokenService`, `BCryptPasswordEncoder` |
| 包路径              | `{module}.{layer}`               | `reservation.usecase`, `reservation.domain`                     |
| Outbound 接口      | `{实体}Repository` / `{能力}Service` | `UserRepository`, `TokenService`                                |
| Infrastructure 包 | `infrastructure.{子包}`            | `infrastructure.persistence`, `infrastructure.security`         |
| 公共模块（全部）         | `Common`                         | `Common`                                                        |
| 公共模块（两模块）        | `Common{模块A}_{模块B}`              | `CommonSeatAndRoom_Reservation`                                 |
| 公共模块（多模块）        | `Common{模块A}_{模块B}_{模块C}`        | `CommonUserAndAuth_Reservation_SystemTask`                      |

## 业务规则

### 预约冲突检测（三项检查）

1. 座位在目标日期+时段是否为 AVAILABLE 状态
2. 同一用户在目标日期+时段是否已有预约（一人一座）
3. 座位在目标日期+时段是否已被他人预约/占用

### 签到规则

- 预约时段未开始：需在时段开始后 30 分钟内签到，超时自动释放
- 当前已在时段内：从当前时间起 30 分钟内签到，超时自动释放
