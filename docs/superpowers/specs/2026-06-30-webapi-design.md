# 校园自习室座位预约管理系统 — Web API 设计文档

## 概述

基于 Clean Architecture 的 RESTful API，采用 Presenter 模式、JWT 认证、Template Method 权限控制。

## 领域实体

| 实体 | 字段 | 说明 |
|---|---|---|
| **User** | id, username, password(hashed), role(STUDENT/ADMIN), name, email | 用户 |
| **StudyRoom** | id, name, location, capacity, status(OPEN/CLOSED) | 自习室 |
| **Seat** | id, roomId, seatNumber, status(AVAILABLE/RESERVED/OCCUPIED/MAINTENANCE) | 座位 |
| **TimeSlot** | id, startTime(08:00/13:00/18:00), endTime(12:00/17:00/22:00) | 固定时段模板 |
| **Reservation** | id, userId, seatId, timeSlotId, date, status(RESERVED/CHECKED_IN/CHECKED_OUT/CANCELLED/EXPIRED), createdAt, checkInAt, checkOutAt | 预约记录 |

### 实体关系

```
StudyRoom 1 ──── N Seat
Seat      1 ──── N Reservation
User      1 ──── N Reservation
TimeSlot  1 ──── N Reservation
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

## 业务规则

### 预约冲突检测
1. 座位在目标日期+时段是否为 AVAILABLE 状态
2. 同一用户在目标日期+时段是否已有预约（一人一座）
3. 座位在目标日期+时段是否已被他人预约/占用

### 签到规则
- 预约成功后，若**当前时间不在预约时段内**：需在时段开始后 30 分钟内签到，超时释放
- 预约成功后，若**当前时间在预约时段内**：从当前时间起 30 分钟内签到，超时释放

## 用例列表

### Auth（UserAndAuth）

| 编号 | 用例 | 描述 | Actor |
|------|------|------|-------|
| UC-01 | RegisterUseCase | 注册账号，默认 STUDENT | 公开 |
| UC-02 | LoginUseCase | 用户名+密码登录，返回 JWT | 公开 |
| UC-03 | GetMeUseCase | 获取当前登录用户信息 | 所有用户 |

### Room & Seat（SeatAndRoom）

| 编号 | 用例 | 描述 | Actor |
|------|------|------|-------|
| UC-04 | ListRoomsUseCase | 获取所有 OPEN 状态的自习室 | 所有用户 |
| UC-05 | ListSeatsUseCase | 获取某自习室所有座位及状态 | 所有用户 |
| UC-06 | ManageRoomsUseCase | 自习室 CRUD + 开关状态 | 管理员 |
| UC-07 | ManageSeatsUseCase | 座位 CRUD + 状态切换（含维护） | 管理员 |

### Reservation（Reservation）

| 编号 | 用例 | 描述 | Actor |
|------|------|------|-------|
| UC-08 | ReserveUseCase | 选择座位+时段+日期，冲突检测通过后创建预约 | 学生 |
| UC-09 | CheckInUseCase | 按签到规则验证并签到，RESERVED→OCCUPIED | 学生 |
| UC-10 | CheckOutUseCase | 主动退座，OCCUPIED→AVAILABLE | 学生 |
| UC-11 | CancelReservationUseCase | 未签到前取消，→CANCELLED，释放座位 | 学生 |
| UC-12 | ListMyReservationsUseCase | 查看自己的预约（当前+历史） | 学生 |
| UC-13 | ManageReservationsUseCase | 管理员查看/取消任意预约 | 管理员 |

### SystemTask（SystemTask）

| 编号 | 用例 | 描述 | Actor |
|------|------|------|-------|
| UC-14 | AutoReleaseUseCase | 定时：扫描超时未签到预约，释放座位 | 系统 |
| UC-15 | GetStatsUseCase | 座位使用率、时段预约量、热门自习室、签到率/违约率 | 管理员 |

## RESTful API 端点

### 认证（公开）

```
POST   /api/auth/register    UC-01
POST   /api/auth/login       UC-02
```

### 认证（需登录）

```
GET    /api/auth/me          UC-03
```

### 自习室

```
GET    /api/rooms            UC-04
GET    /api/rooms/{id}/seats UC-05
POST   /api/admin/rooms      UC-06
PUT    /api/admin/rooms/{id} UC-06
DELETE /api/admin/rooms/{id} UC-06
```

### 座位

```
POST   /api/admin/seats      UC-07
PUT    /api/admin/seats/{id} UC-07
DELETE /api/admin/seats/{id} UC-07
```

### 预约

```
POST   /api/reservations          UC-08
POST   /api/reservations/{id}/check-in   UC-09
POST   /api/reservations/{id}/check-out  UC-10
DELETE /api/reservations/{id}     UC-11
GET    /api/reservations/my       UC-12
GET    /api/admin/reservations    UC-13
```

### 统计

```
GET    /api/admin/stats           UC-15
```

## 架构设计

### 用例继承体系（Template Method）

```
AuthUseCase (abstract)
  TokenValidator + UserRepository 注入
  authenticate(token) → authorize(user, request) → doExecute(user, request)

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
  └── (不继承的公开用例)
        RegisterUseCase, LoginUseCase, GetMeUseCase
        ListRoomsUseCase, ListSeatsUseCase
```

### 模式：UseCase 内嵌 Input/Output/Presenter

```java
public abstract class AuthUseCase<R extends AuthUseCase.AuthRequest> {

    // 构造函数注入
    @Inject protected TokenValidator tokenValidator;
    @Inject protected UserRepository userRepository;

    // 内嵌接口（子类 Request 用 record 实现）
    public interface AuthRequest {
        String token();
    }

    private User authenticate(String token) { ... }
    protected void authorize(User user, R request) { /* hook */ }
    protected abstract void doExecute(User user, R request);

    public void execute(R request) {
        User user = authenticate(request.token());
        authorize(user, request);
        doExecute(user, request);
    }
}
```

### 模式：Presenter 构造函数注入 + ThreadLocal

- **Output** — `execute()` 的返回值，精简，仅含业务标识字段
- **Presenter** — 需要展示的数据直接通过方法形参传入，参数尽量少
- **Resource** — 调用 `execute()` 拿到 Output，通过 `presenter.getResponse()` 获取 HTTP 响应

```java
// UseCase 层 — 定义接口
public class ReserveUseCase extends StudentAuthUseCase<ReserveUseCase.Request> {

    @Inject protected Presenter presenter;  // ← 构造函数注入
    @Inject protected ReservationRepository reservationRepo;
    @Inject protected SeatRepository seatRepo;

    public interface Presenter {
        void success(String reservationId, String seatNumber, String timeSlot);
        void seatNotAvailable(String seatId, String timeSlot);
        void duplicateReservation(String existingId);
    }

    public record Output(String reservationId) {}  // ← execute() 返回值，精简

    public record Request(String token, String seatId, String timeSlotId, LocalDate date)
        implements AuthRequest {}                    // ← record 实现 AuthRequest 接口

    @Override
    protected Output doExecute(User user, Request req) {
        // 冲突检测 ...
        Reservation r = reservationRepo.save(...);
        presenter.success(r.getId(), r.getSeatNumber(), r.getTimeSlotLabel());
        return new Output(r.getId());
    }
}

// WebApi 层 — 实现接口
@Singleton
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

    public Response getResponse() { return current.get(); }
}

// Resource 层
@Path("/reservations")
public class ReservationResource {
    @Inject private ReserveUseCase reserveUseCase;
    @Inject private WebApiReservationPresenter presenter;

    @POST
    public Response reserve(@HeaderParam("Authorization") String auth, ReserveInput input) {
        ReserveUseCase.Output output = reserveUseCase.execute(
            new ReserveUseCase.Request(extractBearer(auth), input.seatId, input.timeSlotId, input.date)
        );
        // output.reservationId 可用于日志、后续操作等
        return presenter.getResponse();
    }
}
```

### 依赖注入绑定（AppBinder）

```java
public class AppBinder extends AbstractBinder {
    @Override protected void configure() {
        // UseCase（PerLookup：每次注入创建新实例）
        bind(ReserveUseCase.class).to(ReserveUseCase.class);
        bind(CheckInUseCase.class).to(CheckInUseCase.class);
        // ...

        // Presenter（Singleton：同实例共享 ThreadLocal）
        bind(WebApiReservationPresenter.class)
            .to(ReserveUseCase.Presenter.class)
            .in(Singleton.class);
        // ...
    }
}
```

### 认证流程

```
客户端 → AuthFilter（仅提取 Bearer Token，不验证）
       → Resource（从 Header 取 token，构建 Request 传给 UseCase）
       → AuthUseCase.authenticate(token)
            → TokenValidator.validate(token)   ← JWT 解析 + 签名验证（Infrastructure）
            → UserRepository.findById(userId)  ← 确认用户存在
       → StudentAuthUseCase.authorize(user)    ← 角色检查
       → ReserveUseCase.doExecute(user, req)   ← 业务逻辑
       → Presenter.success(output)             ← 写入 Response
       → presenter.getResponse()               ← Resource 取回
```

### 模块职责

| 模块 | 层 | 内容 |
|---|---|---|
| **UserAndAuth** | domain | User, UserRole |
| | outbound | UserRepository, TokenValidator |
| | usecase | RegisterUseCase, LoginUseCase, GetMeUseCase, AuthUseCase(abstract), StudentAuthUseCase, AdminAuthUseCase |
| **SeatAndRoom** | domain | StudyRoom, Seat, SeatStatus, TimeSlot |
| | outbound | RoomRepository, SeatRepository, TimeSlotRepository |
| | usecase | ListRoomsUseCase, ListSeatsUseCase, ManageRoomsUseCase, ManageSeatsUseCase |
| **Reservation** | domain | Reservation, ReservationStatus |
| | outbound | ReservationRepository |
| | usecase | ReserveUseCase, CheckInUseCase, CheckOutUseCase, CancelReservationUseCase, ListMyReservationsUseCase, ManageReservationsUseCase |
| **SystemTask** | domain | Stats (value object) |
| | outbound | StatsRepository |
| | usecase | AutoReleaseUseCase, GetStatsUseCase |
| **Infrastructure** | | JwtTokenValidator, JdbcUserRepo, JdbcRoomRepo, JdbcSeatRepo, JdbcReservationRepo, JdbcStatsRepo, AutoReleaseScheduler |
| **WebApi** | resource | AuthResource, RoomResource, SeatResource, ReservationResource, AdminResource, StatsResource |
| | filter | CorsFilter, AuthFilter |
| | presenter | WebApiXxxPresenter（各 UseCase 对应的实现） |
| | binder | AppBinder |

## 数据统计指标

- 座位使用率（按天/周/月）
- 各时段预约量统计
- 热门自习室排名（按预约量）
- 个人签到率（签到次数/预约次数）
- 违约率（超时未签到次数/预约次数）
