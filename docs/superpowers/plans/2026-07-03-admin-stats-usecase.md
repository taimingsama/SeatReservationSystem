# UC-15 数据统计（管理员）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 5 个管理员数据统计 API（今日数据），全部放在 SystemTask 模块，遵循 Clean Architecture + Template Method + Presenter 模式。

**Architecture:** 5 个独立 UseCase 各继承 AdminAuthUseCase，注入现有 Repository 做内存聚合；WebApiStatsPresenter 实现 5 个 Presenter 接口；AdminStatsResource 暴露 5 个端点；SystemTaskBinder 管理 DI 绑定。

**Tech Stack:** Java 17, Jakarta CDI (`@Inject`/`@Singleton`), JAX-RS (Jersey 3.1.7), HK2 DI, JUnit Jupiter 5

## Global Constraints

- 分支: `feature_管理员获取统计`，不使用 worktree
- 遵循现有 Clean Architecture 模式: Domain → Outbound → UseCase → Presenter → Resource
- 全部查询今日 (`LocalDate.now()`)，无需查询参数
- 管理员权限（继承 AdminAuthUseCase），401/403 由 AuthUseCase 基类和 WebApiAuthPresenter 处理
- Presenter 使用 ResponseContext（ThreadLocal）写入 HTTP Response
- 中文错误消息
- 单元测试使用 Stub 模式（不引入 Mock 框架）
- 集成测试使用 JerseyTest + InMemory 实现

---

## File Structure

| 层 | 文件 | 职责 |
|---|---|---|
| Outbound | `SeatRepository.java` | 新增 `findAll()` |
| Outbound | `RoomRepository.java` | 新增 `findAll()` |
| Infra | `InMemorySeatRepo.java` | 实现 `findAll()` |
| Infra | `InMemoryRoomRepo.java` | 实现 `findAll()` |
| Infra | `StubSeatRepo.java` | 实现 `findAll()` |
| UseCase | `GetSeatUsageStatsUseCase.java` | 座位使用率统计 |
| UseCase | `GetTimeSlotStatsUseCase.java` | 时段预约量统计 |
| UseCase | `GetPopularRoomsStatsUseCase.java` | 热门自习室排名 |
| UseCase | `GetCheckInRateStatsUseCase.java` | 签到率统计 |
| UseCase | `GetNoShowRateStatsUseCase.java` | 违约率统计 |
| Test | `GetSeatUsageStatsUseCaseTest.java` | 座位使用率单元测试 |
| Test | `GetTimeSlotStatsUseCaseTest.java` | 时段预约量单元测试 |
| Test | `GetPopularRoomsStatsUseCaseTest.java` | 热门自习室单元测试 |
| Test | `GetCheckInRateStatsUseCaseTest.java` | 签到率单元测试 |
| Test | `GetNoShowRateStatsUseCaseTest.java` | 违约率单元测试 |
| Presenter | `WebApiStatsPresenter.java` | 实现 5 个 Presenter 接口 |
| Resource | `AdminStatsResource.java` | 5 个 GET 端点 |
| Binder | `SystemTaskBinder.java` | DI 绑定 |
| Config | `SystemTask/pom.xml` | 模块依赖 |
| Config | `AppConfig.java` | 注册 Resource + Binder |
| Test | `AdminStatsIntegrationTest.java` | 集成测试 |

---

## 实现顺序总览

```
Task 1-2:   Repository 接口 + InMemory 实现 (findAll)
Task 3:     SystemTask pom.xml 依赖
Task 4-5:   GetSeatUsageStatsUseCase + 单元测试
Task 6-7:   GetTimeSlotStatsUseCase + 单元测试
Task 8-9:   GetPopularRoomsStatsUseCase + 单元测试
Task 10-11: GetCheckInRateStatsUseCase + 单元测试
Task 12-13: GetNoShowRateStatsUseCase + 单元测试
Task 14:    WebApiStatsPresenter
Task 15:    AdminStatsResource (5 个端点)
Task 16:    SystemTaskBinder + AppConfig 注册
Task 17:    集成测试 (8 用例)
Task 18:    全量验证
```

---

### Task 1: Repository 接口新增 findAll()

**Files:**
- Modify: `Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/outbound/SeatRepository.java`
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/RoomRepository.java`

**Produces:** `SeatRepository.findAll() -> List<Seat>`, `RoomRepository.findAll() -> List<StudyRoom>`

- [ ] **Step 1: SeatRepository 新增 findAll()**

在 `findByRoomId` 后面添加：
```java
    List<Seat> findAll();
```

- [ ] **Step 2: RoomRepository 新增 findAll()**

在 `findByName` 后面添加：
```java
    List<StudyRoom> findAll();
```

- [ ] **Step 3: Commit**

```bash
git add Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/outbound/SeatRepository.java
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/RoomRepository.java
git commit -m "feat(outbound): SeatRepository/RoomRepository 新增 findAll()"
```

---

### Task 2: InMemory/Stub 实现 findAll()

**Files:**
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemorySeatRepo.java`
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepo.java`
- Modify: `Common_Reservation_SeatAndRoom_Test_Infrastructure/src/main/java/org/cleancoders/common_reservation_seatandroom_test_infrastructure/StubSeatRepo.java`

- [ ] **Step 1: InMemorySeatRepo 实现 findAll()**

在 `findByRoomId` 后面添加：
```java
    @Override
    public List<Seat> findAll() {
        return List.copyOf(store.values());
    }
```

- [ ] **Step 2: InMemoryRoomRepo 实现 findAll()**

在 `findByName` 后面添加：
```java
    @Override
    public List<StudyRoom> findAll()
    {
        return List.copyOf(rooms.values());
    }
```

- [ ] **Step 3: StubSeatRepo 实现 findAll()**

在 `findByRoomId` 后面添加：
```java
    @Override
    public List<Seat> findAll()
    {
        return List.copyOf(seats.values());
    }
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -pl Infrastructure -am -q
```

预期: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemorySeatRepo.java
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryRoomRepo.java
git add Common_Reservation_SeatAndRoom_Test_Infrastructure/src/main/java/org/cleancoders/common_reservation_seatandroom_test_infrastructure/StubSeatRepo.java
git commit -m "feat(infra): InMemory/Stub SeatRepo/RoomRepo 实现 findAll()"
```

---

### Task 3: SystemTask 模块 pom.xml 配置

**Files:**
- Modify: `SystemTask/pom.xml`

- [ ] **Step 1: 替换 pom.xml**

将 `SystemTask/pom.xml` 替换为含完整依赖的 pom.xml，新增 Common, Common_Reservation_SeatAndRoom, Reservation, SeatAndRoom 主依赖，以及 Common_Test_Infrastructure, Common_Reservation_SeatAndRoom_Test_Infrastructure, JUnit Jupiter 测试依赖。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.cleancoders</groupId>
        <artifactId>SeatReservationSystem</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>SystemTask</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Common_Reservation_SeatAndRoom</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Reservation</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>SeatAndRoom</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Common_Test_Infrastructure</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.cleancoders</groupId>
            <artifactId>Common_Reservation_SeatAndRoom_Test_Infrastructure</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl SystemTask -am -q
```

预期: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add SystemTask/pom.xml
git commit -m "feat(systemtask): 添加模块依赖 Common/Reservation/SeatAndRoom"
```

---

### Task 4: GetSeatUsageStatsUseCase（今日座位使用率）

**Files:**
- Create: `SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetSeatUsageStatsUseCase.java`

**Produces:** `GetSeatUsageStatsUseCase.Presenter`, `Request`, `Output`

- [ ] **Step 1: 创建目录并编写 UseCase**

```bash
mkdir -p SystemTask/src/main/java/org/cleancoders/systemtask/usecase
```

```java
package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UC-15: 今日座位使用率统计（管理员）。
 * 统计当天被占用（RESERVED/OCCUPIED）的座位数占总可用座位（非 REMOVED）的比例。
 */
public class GetSeatUsageStatsUseCase
        extends AdminAuthUseCase<GetSeatUsageStatsUseCase.Request, GetSeatUsageStatsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    SeatRepository seatRepo;

    @Inject
    ReservationRepository reservationRepo;

    public interface Presenter
    {
        void presentSeatUsage(LocalDate date, int totalSeats, int usedSeats, double usageRate);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(LocalDate date, int totalSeats, int usedSeats, double usageRate) {}

    @Override
    protected Output doExecute(User user, Request req)
    {
        LocalDate today = LocalDate.now();

        List<Seat> allSeats = seatRepo.findAll();
        int totalSeats = (int) allSeats.stream()
                .filter(s -> s.status() != SeatStatus.REMOVED)
                .count();

        List<Reservation> todayReservations = reservationRepo.findAll().stream()
                .filter(r -> r.date().equals(today))
                .toList();

        Set<String> usedSeatIds = todayReservations.stream()
                .filter(r -> r.status() == ReservationStatus.RESERVED
                        || r.status() == ReservationStatus.CHECKED_IN)
                .map(Reservation::seatId)
                .collect(Collectors.toSet());
        int usedSeats = usedSeatIds.size();

        double usageRate = totalSeats == 0 ? 0.0
                : Math.round((double) usedSeats / totalSeats * 1000.0) / 1000.0;

        presenter.presentSeatUsage(today, totalSeats, usedSeats, usageRate);
        return new Output(today, totalSeats, usedSeats, usageRate);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl SystemTask -am -q
```

- [ ] **Step 3: Commit**

```bash
git add SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetSeatUsageStatsUseCase.java
git commit -m "feat(systemtask): GetSeatUsageStatsUseCase 今日座位使用率"
```

---

### Task 5: GetSeatUsageStatsUseCase 单元测试

**Files:**
- Create: `SystemTask/src/test/java/org/cleancoders/systemtask/usecase/GetSeatUsageStatsUseCaseTest.java`

**Consumes:** `GetSeatUsageStatsUseCase` (Task 4), `StubTokenService`, `StubUserRepo`, `StubSeatRepo`

- [ ] **Step 1: 创建测试目录**

```bash
mkdir -p SystemTask/src/test/java/org/cleancoders/systemtask/usecase
```

- [ ] **Step 2: 编写测试类 (5 个用例)**

测试类包含:
- 内嵌 `StubReservationRepo` implements ReservationRepository (所有方法)
- 内嵌 `StubPresenter` implements GetSeatUsageStatsUseCase.Presenter, AdminAuthUseCase.Presenter, AuthUseCase.Presenter
- `shouldReturnCorrectUsageRate`: 4 座位 (1 REMOVED), s1/s2 被占用 → total=3, used=2, rate=0.667
- `shouldReturnZeroWhenNoSeats`: 无座位 → 全部返回 0
- `shouldReturnZeroWhenNoTodayReservations`: 昨天的不计数
- `shouldDeduplicateSeats`: 同一座位多预约 → usedSeats=1
- `shouldReturn403WhenNotAdmin`: STUDENT 被拒绝

关键代码模式:
```java
@BeforeEach
void setUp() {
    useCase = new GetSeatUsageStatsUseCase();
    seatRepo = new StubSeatRepo();
    reservationRepo = new StubReservationRepo();
    presenter = new StubPresenter();
    tokenService = new StubTokenService();
    userRepo = new StubUserRepo();

    useCase.presenter = presenter;
    useCase.seatRepo = seatRepo;
    useCase.reservationRepo = reservationRepo;
    useCase.tokenService = tokenService;
    useCase.userRepo = userRepo;
    // 必须设置基类的 presenter 字段
    ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
    ((AuthUseCase<?, ?>) useCase).presenter = presenter;

    User admin = new User("admin-1", "admin", "pass", UserRole.ADMIN, "Admin", "admin@test.com");
    userRepo.addUser(admin);
    tokenService.setValidUser(admin);
}
```

- [ ] **Step 3: 运行测试**

```bash
mvn test -pl SystemTask -am -Dtest=GetSeatUsageStatsUseCaseTest -DfailIfNoTests=false
```

预期: 5 tests PASS

- [ ] **Step 4: Commit**

```bash
git add SystemTask/src/test/java/org/cleancoders/systemtask/usecase/GetSeatUsageStatsUseCaseTest.java
git commit -m "test(systemtask): GetSeatUsageStatsUseCaseTest (5 用例)"
```

---

### Task 6: GetTimeSlotStatsUseCase（各时段预约量）

**Files:**
- Create: `SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetTimeSlotStatsUseCase.java`

**Interfaces:**
- `TimeSlotStatItem(String timeSlotId, String label, long count)` (内嵌 record)
- `Presenter.presentTimeSlotStats(LocalDate date, List<TimeSlotStatItem> items)`
- `Request(String token)`, `Output(LocalDate date, List<TimeSlotStatItem> items)`

**统计逻辑:** timeSlotRepo.findAll() → 今日预约按 timeSlotId groupBy 计数 → merge

```java
// Core logic in doExecute():
Map<String, Long> countsBySlot = todayReservations.stream()
        .collect(Collectors.groupingBy(Reservation::timeSlotId, Collectors.counting()));

List<TimeSlotStatItem> items = timeSlotRepo.findAll().stream()
        .map(ts -> new TimeSlotStatItem(
                ts.id(), ts.label(),
                countsBySlot.getOrDefault(ts.id(), 0L)))
        .toList();
```

- [ ] **Step 1: 编写 UseCase + 编译验证** `mvn compile -pl SystemTask -am -q`
- [ ] **Step 2: Commit** `git commit -m "feat(systemtask): GetTimeSlotStatsUseCase"`

---

### Task 7: GetTimeSlotStatsUseCase 单元测试

**Files:**
- Create: `SystemTask/src/test/java/org/cleancoders/systemtask/usecase/GetTimeSlotStatsUseCaseTest.java`

**测试用例 (3):**
1. `shouldCountReservationsPerTimeSlot` — ts-1: 2, ts-2: 1, ts-3: 0
2. `shouldReturnZeroWhenNoTodayReservations` — 全部为0
3. `shouldReturn403WhenNotAdmin`

**依赖:** 复用 `GetSeatUsageStatsUseCaseTest.StubReservationRepo`, 新增内嵌 `StubTimeSlotRepo` implements TimeSlotRepository

- [ ] **Step 1: 编写测试 + 运行** → 3 PASS
- [ ] **Step 2: Commit** `git commit -m "test(systemtask): GetTimeSlotStatsUseCaseTest (3)"`

---

### Task 8: GetPopularRoomsStatsUseCase（热门自习室排名）

**Files:**
- Create: `SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetPopularRoomsStatsUseCase.java`

**Interfaces:**
- `PopularRoomItem(String roomId, String roomName, long reservationCount)`
- `Presenter.presentPopularRooms(LocalDate date, List<PopularRoomItem> items)`

**统计逻辑:** seatToRoom 映射 → 今日预约按 roomId 分组 → 关联名称 → 降序

```java
// Core logic:
Map<String, String> seatToRoom = seatRepo.findAll().stream()
        .collect(Collectors.toMap(Seat::id, Seat::roomId));

Map<String, Long> countsByRoom = todayReservations.stream()
        .map(r -> seatToRoom.get(r.seatId()))
        .filter(roomId -> roomId != null)
        .collect(Collectors.groupingBy(roomId -> roomId, Collectors.counting()));

Map<String, String> roomNames = roomRepo.findAll().stream()
        .collect(Collectors.toMap(StudyRoom::id, StudyRoom::name));

List<PopularRoomItem> items = countsByRoom.entrySet().stream()
        .map(e -> new PopularRoomItem(e.getKey(),
                roomNames.getOrDefault(e.getKey(), "未知"), e.getValue()))
        .sorted(Comparator.comparingLong(PopularRoomItem::reservationCount).reversed())
        .toList();
```

- [ ] **Step 1: 编写 UseCase + 编译** `mvn compile -pl SystemTask -am -q`
- [ ] **Step 2: Commit** `git commit -m "feat(systemtask): GetPopularRoomsStatsUseCase"`

---

### Task 9: GetPopularRoomsStatsUseCase 单元测试

**Files:**
- Create: `SystemTask/src/test/java/org/cleancoders/systemtask/usecase/GetPopularRoomsStatsUseCaseTest.java`

**测试用例 (3):**
1. `shouldRankRoomsByReservationCountDescending` — room-1: 3, room-2: 1
2. `shouldReturnEmptyWhenNoTodayReservations`
3. `shouldReturn403WhenNotAdmin`

**依赖:** 新增内嵌 `StubRoomRepo` implements RoomRepository

- [ ] **Step 1: 编写测试 + 运行** → 3 PASS
- [ ] **Step 2: Commit** `git commit -m "test(systemtask): GetPopularRoomsStatsUseCaseTest (3)"`

---

### Task 10: GetCheckInRateStatsUseCase（签到率）

**Files:**
- Create: `SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetCheckInRateStatsUseCase.java`

**Interfaces:**
- `Presenter.presentCheckInRate(LocalDate date, int totalReservations, int checkedIn, double checkInRate)`
- `Request(String token)`, `Output(LocalDate date, int totalReservations, int checkedIn, double checkInRate)`

**统计逻辑:** 今日预约总数 → CHECKED_IN + CHECKED_OUT 占比，分母0返回0.0

- [ ] **Step 1: 编写 UseCase + 编译** `mvn compile -pl SystemTask -am -q`
- [ ] **Step 2: Commit** `git commit -m "feat(systemtask): GetCheckInRateStatsUseCase"`

---

### Task 11: GetCheckInRateStatsUseCase 单元测试

**Files:**
- Create: `SystemTask/src/test/java/org/cleancoders/systemtask/usecase/GetCheckInRateStatsUseCaseTest.java`

**测试用例 (4):**
1. 正常签到率 2/4 = 0.5
2. CHECKED_OUT 也算已签到 (checkedIn=1)
3. 空数据 → 0
4. 403

- [ ] **Step 1: 编写测试 + 运行** → 4 PASS
- [ ] **Step 2: Commit** `git commit -m "test(systemtask): GetCheckInRateStatsUseCaseTest (4)"`

---

### Task 12: GetNoShowRateStatsUseCase（违约率）

**Files:**
- Create: `SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetNoShowRateStatsUseCase.java`

**Interfaces:**
- `Presenter.presentNoShowRate(LocalDate date, int totalReservations, int expired, double noShowRate)`
- `Request(String token)`, `Output(LocalDate date, int totalReservations, int expired, double noShowRate)`

**统计逻辑:** 今日预约总数 → EXPIRED 占比，分母0返回0.0

- [ ] **Step 1: 编写 UseCase + 编译** `mvn compile -pl SystemTask -am -q`
- [ ] **Step 2: Commit** `git commit -m "feat(systemtask): GetNoShowRateStatsUseCase"`

---

### Task 13: GetNoShowRateStatsUseCase 单元测试

**Files:**
- Create: `SystemTask/src/test/java/org/cleancoders/systemtask/usecase/GetNoShowRateStatsUseCaseTest.java`

**测试用例 (4):**
1. 正常违约率 1/3 = 0.333
2. CANCELLED 不算 EXPIRED (expired=0)
3. 空数据 → 0
4. 403

- [ ] **Step 1: 编写测试 + 运行** → 4 PASS
- [ ] **Step 2: Commit** `git commit -m "test(systemtask): GetNoShowRateStatsUseCaseTest (4)"`

---

### Task 14: WebApiStatsPresenter

**Files:**
- Create: `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiStatsPresenter.java`

**Consumes:** 5 个 UseCase 的 Presenter 接口 (Tasks 4, 6, 8, 10, 12)

```java
@Singleton
public class WebApiStatsPresenter extends WebApiPresenter implements
        GetSeatUsageStatsUseCase.Presenter,
        GetTimeSlotStatsUseCase.Presenter,
        GetPopularRoomsStatsUseCase.Presenter,
        GetCheckInRateStatsUseCase.Presenter,
        GetNoShowRateStatsUseCase.Presenter
{
    // 每个 presentXxx 方法:
    // 1. 构建 LinkedHashMap body (保持字段顺序)
    // 2. responseContext.set(Response.ok(body).build())
}
```

每个 presenter 方法的响应格式遵循设计文档，使用 `LinkedHashMap` 保持字段顺序，日期字段用 `date.toString()`。

- [ ] **Step 1: 编写 Presenter + 编译** `mvn compile -pl WebApi -am -q`
- [ ] **Step 2: Commit** `git commit -m "feat(webapi): WebApiStatsPresenter 实现 5 个统计 Presenter 接口"`

---

### Task 15: AdminStatsResource

**Files:**
- Create: `WebApi/src/main/java/org/cleancoders/web/resource/AdminStatsResource.java`

```java
@Path("/admin/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin Stats", description = "管理员数据统计接口")
public class AdminStatsResource
{
    @Inject GetSeatUsageStatsUseCase getSeatUsageStatsUseCase;
    @Inject GetTimeSlotStatsUseCase getTimeSlotStatsUseCase;
    @Inject GetPopularRoomsStatsUseCase getPopularRoomsStatsUseCase;
    @Inject GetCheckInRateStatsUseCase getCheckInRateStatsUseCase;
    @Inject GetNoShowRateStatsUseCase getNoShowRateStatsUseCase;
    @Inject ResponseContext responseContext;

    // 5 个 GET 端点: /seat-usage, /time-slot, /popular-rooms, /check-in-rate, /no-show-rate
    // 每个端点: useCase.execute(new XxxUseCase.Request(authCookie)); return responseContext.get();
    // 带 @Operation + @ApiResponses(200/401/403)
}
```

- [ ] **Step 1: 编写 Resource + 编译** `mvn compile -pl WebApi -am -q`
- [ ] **Step 2: Commit** `git commit -m "feat(webapi): AdminStatsResource 5 个统计端点"`

---

### Task 16: SystemTaskBinder + AppConfig 注册

**Files:**
- Create: `WebApi/src/main/java/org/cleancoders/web/binder/SystemTaskBinder.java`
- Modify: `WebApi/src/main/java/org/cleancoders/web/AppConfig.java`

**SystemTaskBinder:**
```java
public class SystemTaskBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        bind(GetSeatUsageStatsUseCase.class).to(GetSeatUsageStatsUseCase.class);
        bind(GetTimeSlotStatsUseCase.class).to(GetTimeSlotStatsUseCase.class);
        bind(GetPopularRoomsStatsUseCase.class).to(GetPopularRoomsStatsUseCase.class);
        bind(GetCheckInRateStatsUseCase.class).to(GetCheckInRateStatsUseCase.class);
        bind(GetNoShowRateStatsUseCase.class).to(GetNoShowRateStatsUseCase.class);

        bind(WebApiStatsPresenter.class)
                .to(GetSeatUsageStatsUseCase.Presenter.class)
                .to(GetTimeSlotStatsUseCase.Presenter.class)
                .to(GetPopularRoomsStatsUseCase.Presenter.class)
                .to(GetCheckInRateStatsUseCase.Presenter.class)
                .to(GetNoShowRateStatsUseCase.Presenter.class)
                .in(Singleton.class);
    }
}
```

**AppConfig.java 修改:**
- import 新增: `import org.cleancoders.web.binder.SystemTaskBinder;`
- getClasses() 新增: `classes.add(AdminStatsResource.class);` + `classes.add(SystemTaskBinder.class);`

- [ ] **Step 1: 编写 + 修改 + 编译** `mvn compile -pl WebApi -am -q`
- [ ] **Step 2: Commit** `git commit -m "feat(webapi): SystemTaskBinder + AppConfig 注册统计端点"`

---

### Task 17: 集成测试

**Files:**
- Create: `WebApi/src/test/java/org/cleancoders/web/resource/AdminStatsIntegrationTest.java`

**8 个测试用例，使用 JerseyTest + InMemory 实现:**

| # | 测试 | 端点 | 预期 |
|---|------|------|------|
| 1 | shouldReturnSeatUsageForAdmin | GET /seat-usage | 200, totalSeats=12, usedSeats=0 |
| 2 | shouldReturnTimeSlotStatsForAdmin | GET /time-slot | 200, 3 个时段 |
| 3 | shouldReturnPopularRoomsForAdmin | GET /popular-rooms | 200, rooms 存在 |
| 4 | shouldReturnCheckInRateForAdmin | GET /check-in-rate | 200, totalReservations=0 |
| 5 | shouldReturnNoShowRateForAdmin | GET /no-show-rate | 200, totalReservations=0 |
| 6 | shouldReturn401WhenNoToken | GET /seat-usage | 401 |
| 7 | shouldReturn403WhenStudent | GET /seat-usage (student token) | 403 |
| 8 | shouldReturn401WhenNoTokenForCheckInRate | GET /check-in-rate | 401 |

**JerseyTest 配置:** 注册 AdminStatsResource, SystemTaskBinder, 和所有现有 Binder。Override 绑定 InMemory repo 实例。

- [ ] **Step 1: 编写测试 + 运行** → 8 PASS
- [ ] **Step 2: Commit** `git commit -m "test(webapi): AdminStatsIntegrationTest (8 用例)"`

---

### Task 18: 全量验证

- [ ] **Step 1: SystemTask 单元测试**

```bash
mvn test -pl SystemTask -am
```

预期: 19 tests PASS (5+3+3+4+4)

- [ ] **Step 2: WebApi 全量测试**

```bash
mvn test -pl WebApi -am
```

预期: ALL TESTS PASS

- [ ] **Step 3: 最终构建**

```bash
mvn clean package -pl WebApi -am -DskipTests
```

预期: BUILD SUCCESS

---

## Summary

| Task | 内容 | 新文件 | 改文件 |
|------|------|--------|--------|
| 1 | Repository 接口 findAll() | 0 | 2 |
| 2 | InMemory/Stub 实现 findAll() | 0 | 3 |
| 3 | SystemTask pom.xml | 0 | 1 |
| 4 | GetSeatUsageStatsUseCase | 1 | 0 |
| 5 | GetSeatUsageStatsUseCaseTest | 1 | 0 |
| 6 | GetTimeSlotStatsUseCase | 1 | 0 |
| 7 | GetTimeSlotStatsUseCaseTest | 1 | 0 |
| 8 | GetPopularRoomsStatsUseCase | 1 | 0 |
| 9 | GetPopularRoomsStatsUseCaseTest | 1 | 0 |
| 10 | GetCheckInRateStatsUseCase | 1 | 0 |
| 11 | GetCheckInRateStatsUseCaseTest | 1 | 0 |
| 12 | GetNoShowRateStatsUseCase | 1 | 0 |
| 13 | GetNoShowRateStatsUseCaseTest | 1 | 0 |
| 14 | WebApiStatsPresenter | 1 | 0 |
| 15 | AdminStatsResource | 1 | 0 |
| 16 | SystemTaskBinder + AppConfig | 1 | 1 |
| 17 | AdminStatsIntegrationTest | 1 | 0 |
| 18 | 全量验证 | 0 | 0 |
| **合计** | | **12** | **7** |
