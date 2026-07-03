# UC-15 数据统计（管理员）— 设计文档

## 概述

为管理员提供 5 个数据统计 API，全部查询今日（`LocalDate.now()`）数据，无需额外参数。按 Clean Architecture 模式拆分为 5 个独立
UseCase，各自继承 `AdminAuthUseCase`，遵循 Template Method + Presenter 模式。

## API 端点

```
GET /api/admin/stats/seat-usage       → 今日座位使用率
GET /api/admin/stats/time-slot        → 今日各时段预约量
GET /api/admin/stats/popular-rooms    → 今日热门自习室排名
GET /api/admin/stats/check-in-rate    → 今日签到率
GET /api/admin/stats/no-show-rate     → 今日违约率
```

全部端点：

- 无需查询参数（统一查今日）
- 需要管理员权限（返回 401/403）
- 使用 `@CookieParam("Authorization")` 传递 JWT

## 响应格式

### 1. 座位使用率

```json
{
  "date": "2026-07-03",
  "totalSeats": 28,
  "usedSeats": 15,
  "usageRate": 0.536
}
```

| 字段           | 说明                                    |
|--------------|---------------------------------------|
| `totalSeats` | 非 REMOVED 状态座位总数                      |
| `usedSeats`  | 今日存在 RESERVED 或 OCCUPIED 状态预约的座位数（去重） |
| `usageRate`  | usedSeats / totalSeats，保留 3 位小数       |

### 2. 各时段预约量

```json
{
  "date": "2026-07-03",
  "timeSlots": [
    {"timeSlotId": "ts-1", "label": "08:00-12:00", "count": 15},
    {"timeSlotId": "ts-2", "label": "13:00-17:00", "count": 22},
    {"timeSlotId": "ts-3", "label": "18:00-22:00", "count": 18}
  ]
}
```

### 3. 热门自习室排名

```json
{
  "date": "2026-07-03",
  "rooms": [
    {"roomId": "room-1", "roomName": "自习室A", "reservationCount": 25},
    {"roomId": "room-3", "roomName": "自习室C", "reservationCount": 18}
  ]
}
```

按 `reservationCount` 降序排列。

### 4. 签到率

```json
{
  "date": "2026-07-03",
  "totalReservations": 50,
  "checkedIn": 30,
  "checkInRate": 0.6
}
```

`checkedIn` = CHECKED_IN + CHECKED_OUT 状态的预约数（退座前必然签到过）。

### 5. 违约率

```json
{
  "date": "2026-07-03",
  "totalReservations": 50,
  "expired": 5,
  "noShowRate": 0.1
}
```

## 架构设计

### 文件结构

**SystemTask 模块（新建 5 个 UseCase）：**

```
SystemTask/src/main/java/.../systemtask/usecase/
  GetSeatUsageStatsUseCase.java
  GetTimeSlotStatsUseCase.java
  GetPopularRoomsStatsUseCase.java
  GetCheckInRateStatsUseCase.java
  GetNoShowRateStatsUseCase.java
```

**WebApi 模块（新建 Resource + Presenter + Binder）：**

```
WebApi/src/main/java/.../web/
  resource/AdminStatsResource.java
  presenter/WebApiStatsPresenter.java
  binder/SystemTaskBinder.java
```

**现有文件修改：**

| 文件                      | 修改内容                                                                  |
|-------------------------|-----------------------------------------------------------------------|
| `SystemTask/pom.xml`    | 添加依赖：Common, Common_Reservation_SeatAndRoom, Reservation, SeatAndRoom |
| `SeatRepository.java`   | 新增 `findAll()` 方法                                                     |
| `RoomRepository.java`   | 新增 `findAll()` 方法                                                     |
| `InMemorySeatRepo.java` | 实现 `findAll()`                                                        |
| `InMemoryRoomRepo.java` | 实现 `findAll()`                                                        |
| `AppConfig.java`        | 注册 `AdminStatsResource` + `SystemTaskBinder`                          |

### UseCase 模式

每个 UseCase 遵循与 `DeleteSeatUseCase` 完全一致的模式：

```java
public class GetXxxStatsUseCase
    extends AdminAuthUseCase<GetXxxStatsUseCase.Request, GetXxxStatsUseCase.Output> {

    @Inject Presenter presenter;
    @Inject ReservationRepository reservationRepo;
    // ... 其他 repo

    public interface Presenter {
        void presentXxx(/* 统计结果参数 */);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(/* 统计结果字段 */) {}

    @Override
    protected Output doExecute(User user, Request req) {
        LocalDate today = LocalDate.now();
        // 从 repo 取数据 → 过滤今日 → 聚合统计 → 调用 presenter
        return new Output(...)
    }
}
```

### 数据获取策略

复用现有 Repository 接口，在 UseCase 层做内存聚合：

| UseCase          | 依赖 Repository                                         | 聚合逻辑                                                                       |
|------------------|-------------------------------------------------------|----------------------------------------------------------------------------|
| GetSeatUsage     | SeatRepository, ReservationRepository                 | seatRepo.findAll() 得总数，reservationRepo.findAll() 过滤今日得已用座位集合               |
| GetTimeSlotStats | TimeSlotRepository, ReservationRepository             | timeSlotRepo.findAll() 得时段列表，reservationRepo.findAll() 过滤今日按 timeSlotId 计数 |
| GetPopularRooms  | RoomRepository, SeatRepository, ReservationRepository | 预约→seatId→roomId 二级映射后 group by + 排序                                       |
| GetCheckInRate   | ReservationRepository                                 | reservationRepo.findAll() 过滤今日，统计 CHECKED_IN + CHECKED_OUT 占比              |
| GetNoShowRate    | ReservationRepository                                 | reservationRepo.findAll() 过滤今日，统计 EXPIRED 占比                               |

### 模块依赖

```
SystemTask
  ├── Common                           (AdminAuthUseCase, User)
  ├── Common_Reservation_SeatAndRoom    (Seat, SeatRepository, TimeSlot, TimeSlotRepository)
  ├── Reservation                      (Reservation, ReservationRepository)
  └── SeatAndRoom                      (StudyRoom, RoomRepository)
```

### Presenter 实现

`WebApiStatsPresenter` 继承 `WebApiPresenter`，实现 5 个 Presenter 接口，使用 `ResponseContext`（ThreadLocal）写入 HTTP 响应。
`SystemTaskBinder` 将其以 Singleton 多绑到 5 个接口上。

## 测试

### 单元测试（5 个 UseCase 各一个 Test 类）

参考 `DeleteSeatUseCaseTest` 的 Stub 模式：

- 使用 `StubTokenService`、`StubUserRepo`、`StubSeatRepo`、`StubTimeSlotRepo`
- 内嵌 `StubPresenter` 实现对应 Presenter 接口 + Auth Presenter 接口
- 手动注入字段，验证 Output 和 Presenter 回调

### 集成测试

追加到 `AdminResourceIntegrationTest`（或新建 `AdminStatsIntegrationTest`）：

- 通过 `TestDataReservationRepo` 预设今日预约数据
- 验证 5 个端点的 HTTP 状态码和 JSON 响应体

## 统计口径说明

- **今日**：`LocalDate.now()`，统计日期等于今天的预约记录
- **usedSeats（已用座位）**：今日有 RESERVED 或 OCCUPIED 状态预约的座位，按 seatId 去重
- **totalSeats（总座位）**：非 REMOVED 状态的所有座位
- **checkedIn（已签到）**：状态为 CHECKED_IN 或 CHECKED_OUT（签到后可能已退座）
- **expired（违约）**：状态为 EXPIRED
- **totalReservations（分母）**：今日所有预约记录总数（不限状态）
- **分母为 0 时**：usageRate/checkInRate/noShowRate 返回 0.0
