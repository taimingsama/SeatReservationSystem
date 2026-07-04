# 按时间段区分座位状态 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `ListSeatsUseCase` 根据查询时段是当前/未来/过去，返回不同粒度的座位状态（当前4状态含 OCCUPIED，未来3状态无 OCCUPIED，过去拒绝查询）。

**Architecture:** 扩展 `ActiveReservationChecker` 接口返回 `Optional<ReservationStatus>` 替代 boolean，重写 `computeEffectiveStatus` 增加时段分类和状态映射逻辑。

**Tech Stack:** Java 17, JUnit 5, HK2 DI

**Spec:** `docs/superpowers/specs/2026-07-04-time-slot-aware-seat-status-design.md`

## Global Constraints

- 所有测试必须通过（`mvn test -pl WebApi -am`）
- 遵循现有代码风格（4空格缩进，中文注释）
- 小步提交，每任务一个 commit

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `SeatAndRoom/.../outbound/ActiveReservationChecker.java` | 修改 | 新增 `getReservationStatus` 方法 |
| `Infrastructure/.../ReservationBasedActiveReservationChecker.java` | 修改 | 实现新方法 |
| `SeatAndRoom/.../usecase/ListSeatsUseCase.java` | 修改 | 重写 `computeEffectiveStatus`，增加时段分类 |
| `SeatAndRoom/.../usecase/ListSeatsUseCaseTest.java` | 修改 | 新增时段区分测试 |
| `WebApi/.../presenter/WebApiRoomPresenter.java` | 修改 | 新增 `pastTimeSlot` 错误响应 |
| `WebApi/.../RoomResourceIntegrationTest.java` | 修改 | 新增集成测试 |

---

### Task 1: 扩展 ActiveReservationChecker 接口

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/ActiveReservationChecker.java`

**Interfaces:**
- Produces: `Optional<ReservationStatus> getReservationStatus(String roomId, int seatId, String timeSlotId, LocalDate date)`

- [ ] **Step 1: 添加新方法和 import**

```java
// 在文件顶部 import 区添加
import org.cleancoders.reservation.domain.ReservationStatus;
import java.time.LocalDate;
import java.util.Optional;

// 在接口中添加方法（hasActiveForSeat 方法之后）
    /**
     * Returns the active reservation status for a seat at the specified time slot and date.
     * @return {@code Optional.empty()} if no active reservation;
     *         {@link ReservationStatus#RESERVED} if reserved but not checked in;
     *         {@link ReservationStatus#CHECKED_IN} if checked in.
     */
    Optional<ReservationStatus> getReservationStatus(String roomId, int seatId, String timeSlotId, LocalDate date);
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl SeatAndRoom -am
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/ActiveReservationChecker.java
git commit -m "feat: ActiveReservationChecker新增getReservationStatus方法"
```

---

### Task 2: 实现 ReservationBasedActiveReservationChecker 新方法

**Files:**
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationChecker.java`

**Interfaces:**
- Consumes: `Optional<ReservationStatus> getReservationStatus(String, int, String, LocalDate)` from Task 1

- [ ] **Step 1: 实现新方法**

在 `isReservedForTimeSlot` 方法之后添加：

```java
    @Override
    public Optional<ReservationStatus> getReservationStatus(String roomId, int seatId, String timeSlotId, LocalDate date) {
        return reservationRepo.findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                roomId, seatId, date, timeSlotId,
                Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN))
                .map(r -> r.status());
    }
```

添加 import：

```java
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;
import java.util.Optional;
```

`ReservationStatus` 和 `Set` 已有 import，无需新增。

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl Infrastructure -am
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationChecker.java
git commit -m "feat: 实现getReservationStatus方法"
```

---

### Task 3: 重写 ListSeatsUseCase.computeEffectiveStatus

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ListSeatsUseCase.java`

**Interfaces:**
- Consumes: `getReservationStatus` from `ActiveReservationChecker` (Task 1)
- Produces: 新的 `computeEffectiveStatus` 逻辑，`PastTimeSlot` presenter 方法

- [ ] **Step 1: 更新 Presenter 接口**

在 `ListSeatsUseCase.Presenter` 接口中添加：

```java
        void roomNotFound(String roomId);

        void pastTimeSlot(String timeSlotId, LocalDate date);
```

- [ ] **Step 2: 重写 computeEffectiveStatus 和 execute**

替换现有的 `computeEffectiveStatus` 方法和 `execute` 中调用部分：

```java
    public Output execute(Request request)
    {
        Optional<StudyRoom> room = roomRepo.findById(request.roomId());
        if (room.isEmpty())
        {
            presenter.roomNotFound(request.roomId());
            return new Output(List.of());
        }
        List<Seat> seats = seatRepo.findByRoomId(request.roomId());

        // 如果指定了时间段，根据预约情况计算有效状态
        if (request.timeSlotId() != null && request.date() != null)
        {
            var timeSlotOpt = timeSlotRepo.findById(request.timeSlotId());
            if (timeSlotOpt.isEmpty())
            {
                presenter.roomNotFound(request.roomId());
                return new Output(List.of());
            }

            TimeSlot timeSlot = timeSlotOpt.get();
            LocalTime slotStart = LocalTime.parse(timeSlot.startTime());
            LocalTime slotEnd = LocalTime.parse(timeSlot.endTime());
            LocalDateTime slotStartDateTime = LocalDateTime.of(request.date(), slotStart);
            LocalDateTime slotEndDateTime = LocalDateTime.of(request.date(), slotEnd);
            LocalDateTime now = getCurrentTime();

            // 过去时段不允许查询
            if (now.isAfter(slotEndDateTime))
            {
                presenter.pastTimeSlot(request.timeSlotId(), request.date());
                return new Output(List.of());
            }

            boolean isCurrent = !now.isBefore(slotStartDateTime) && now.isBefore(slotEndDateTime);

            seats = seats.stream()
                    .map(s -> computeEffectiveStatus(s, request.timeSlotId(), request.date(), isCurrent))
                    .toList();
        }

        presenter.presentSeats(room.get(), seats);
        return new Output(seats);
    }

    /**
     * Returns the current time. Exposed for testability.
     */
    protected LocalDateTime getCurrentTime()
    {
        return LocalDateTime.now();
    }

    /**
     * 根据时间段预约情况计算座位的实际状态。
     * 当前时段：AVAILABLE → OCCUPIED(CHECKED_IN) / RESERVED / AVAILABLE
     * 未来时段：AVAILABLE → RESERVED / AVAILABLE（无 OCCUPIED）
     * MAINTENANCE / REMOVED 保持不变。
     */
    private Seat computeEffectiveStatus(Seat seat, String timeSlotId, LocalDate date, boolean isCurrent)
    {
        // 非 AVAILABLE 的静态状态保持不变
        if (seat.status() != SeatStatus.AVAILABLE)
        {
            return seat;
        }

        // 查询该时段的活跃预约
        var reservationStatus = activeReservationChecker.getReservationStatus(
                seat.roomId(), seat.id(), timeSlotId, date);

        if (reservationStatus.isEmpty())
        {
            return seat; // AVAILABLE
        }

        if (isCurrent && reservationStatus.get() == ReservationStatus.CHECKED_IN)
        {
            return new Seat(seat.id(), seat.roomId(), SeatStatus.OCCUPIED);
        }

        // RESERVED (or CHECKED_IN in future slot → map to RESERVED)
        return new Seat(seat.id(), seat.roomId(), SeatStatus.RESERVED);
    }
```

添加 import：

```java
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.seatandroom.domain.TimeSlot;
import java.time.LocalDateTime;
import java.time.LocalTime;
```

- [ ] **Step 3: 注入 TimeSlotRepository**

在类中添加：

```java
    @Inject
    TimeSlotRepository timeSlotRepo;
```

添加 import：

```java
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -pl SeatAndRoom -am
```

Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ListSeatsUseCase.java
git commit -m "feat: ListSeatsUseCase按时段分类返回座位状态（当前4状态/未来3状态/过去400）"
```

---

### Task 4: 更新单元测试

**Files:**
- Modify: `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/ListSeatsUseCaseTest.java`

- [ ] **Step 1: 更新 StubPresenter 实现新方法**

在 `StubPresenter` 类中添加：

```java
        final AtomicReference<String> pastTimeSlotId = new AtomicReference<>();
        final AtomicReference<LocalDate> pastTimeSlotDate = new AtomicReference<>();

        @Override
        public void pastTimeSlot(String timeSlotId, LocalDate date)
        {
            pastTimeSlotId.set(timeSlotId);
            pastTimeSlotDate.set(date);
        }
```

- [ ] **Step 2: 更新 StubActiveReservationChecker**

替换现有的 `StubActiveReservationChecker`：

```java
    static class StubActiveReservationChecker implements ActiveReservationChecker
    {
        private final java.util.Map<String, ReservationStatus> statuses = new java.util.HashMap<>();

        void setStatus(String roomId, int seatId, String timeSlotId, LocalDate date, ReservationStatus status)
        {
            statuses.put(roomId + ":" + seatId + ":" + timeSlotId + ":" + date, status);
        }

        @Override
        public boolean hasActiveForSeat(String roomId, int seatId)
        {
            return false;
        }

        @Override
        public boolean isReservedForTimeSlot(String roomId, int seatId, String timeSlotId, LocalDate date)
        {
            return statuses.containsKey(roomId + ":" + seatId + ":" + timeSlotId + ":" + date);
        }

        @Override
        public Optional<ReservationStatus> getReservationStatus(String roomId, int seatId, String timeSlotId, LocalDate date)
        {
            return Optional.ofNullable(statuses.get(roomId + ":" + seatId + ":" + timeSlotId + ":" + date));
        }
    }
```

添加 import：

```java
import org.cleancoders.reservation.domain.ReservationStatus;
import java.util.Optional;
```

- [ ] **Step 3: 更新 setUp 注入**

在 setUp 中添加：

```java
        useCase.timeSlotRepo = timeSlotRepo;
```

- [ ] **Step 4: 重写现有测试**

将 "shouldMarkAvailableSeatAsReservedWhenTimeSlotProvided" 测试替换为新的场景测试。在 `shouldIncludeAllSeatStatuses` 测试之后、`// --- Stubs ---` 之前添加：

```java
    @Test
    void shouldReturnOccupiedForCheckedInDuringCurrentSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.AVAILABLE);
        seatRepo.save(s1);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));

        var checker = (StubActiveReservationChecker) useCase.activeReservationChecker;
        checker.setStatus("room-1", 1, "ts-1", LocalDate.of(2026, 7, 4), ReservationStatus.CHECKED_IN);

        // Mock current time to be within the slot (10:00)
        useCase = new ListSeatsUseCase()
        {
            @Override
            protected LocalDateTime getCurrentTime()
            {
                return LocalDateTime.of(2026, 7, 4, 10, 0);
            }
        };
        useCase.roomRepo = roomRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.activeReservationChecker = checker;
        useCase.presenter = presenter;

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-1", LocalDate.of(2026, 7, 4)));

        assertEquals(1, output.seats().size());
        assertEquals(SeatStatus.OCCUPIED, output.seats().get(0).status());
    }

    @Test
    void shouldReturnReservedForCheckedInDuringFutureSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.AVAILABLE);
        seatRepo.save(s1);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-2", "13:00", "17:00", "下午 13:00-17:00"));

        var checker = (StubActiveReservationChecker) useCase.activeReservationChecker;
        checker.setStatus("room-1", 1, "ts-2", LocalDate.of(2026, 7, 4), ReservationStatus.CHECKED_IN);

        // Mock current time to be before the slot (10:00, future slot)
        useCase = new ListSeatsUseCase()
        {
            @Override
            protected LocalDateTime getCurrentTime()
            {
                return LocalDateTime.of(2026, 7, 4, 10, 0);
            }
        };
        useCase.roomRepo = roomRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.activeReservationChecker = checker;
        useCase.presenter = presenter;

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-2", LocalDate.of(2026, 7, 4)));

        assertEquals(1, output.seats().size());
        // CHECKED_IN in future → mapped to RESERVED
        assertEquals(SeatStatus.RESERVED, output.seats().get(0).status());
    }

    @Test
    void shouldRejectPastTimeSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));

        // Mock current time to be after the slot (14:00, past slot)
        useCase = new ListSeatsUseCase()
        {
            @Override
            protected LocalDateTime getCurrentTime()
            {
                return LocalDateTime.of(2026, 7, 4, 14, 0);
            }
        };
        useCase.roomRepo = roomRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.activeReservationChecker = new StubActiveReservationChecker();
        useCase.presenter = presenter;

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-1", LocalDate.of(2026, 7, 4)));

        assertTrue(output.seats().isEmpty());
        assertEquals("ts-1", presenter.pastTimeSlotId.get());
    }

    @Test
    void shouldKeepMaintenanceRegardlessOfTimeSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.MAINTENANCE);
        seatRepo.save(s1);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));

        // Current slot
        useCase = new ListSeatsUseCase()
        {
            @Override
            protected LocalDateTime getCurrentTime()
            {
                return LocalDateTime.of(2026, 7, 4, 10, 0);
            }
        };
        useCase.roomRepo = roomRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.activeReservationChecker = new StubActiveReservationChecker();
        useCase.presenter = presenter;

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-1", LocalDate.of(2026, 7, 4)));

        assertEquals(1, output.seats().size());
        assertEquals(SeatStatus.MAINTENANCE, output.seats().get(0).status());
    }
```

添加 import：

```java
import org.cleancoders.seatandroom.domain.TimeSlot;
import java.time.LocalDateTime;
```

- [ ] **Step 5: 运行测试**

```bash
mvn test -pl SeatAndRoom -am -Dtest=ListSeatsUseCaseTest
```

Expected: Tests run: 10, Failures: 0 (原来6个 + 新增4个)

- [ ] **Step 6: 提交**

```bash
git add SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/ListSeatsUseCaseTest.java
git commit -m "test: ListSeatsUseCase新增时段区分测试"
```

---

### Task 5: 更新 WebApiRoomPresenter

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java`

- [ ] **Step 1: 实现 pastTimeSlot 方法**

在 `roomNotFound` 方法之后添加：

```java
    @Override
    public void pastTimeSlot(String timeSlotId, LocalDate date)
    {
        responseContext.set(Response.status(400).entity(
                new ErrorResponse("不能查询过去时段")).build());
    }
```

添加 import：

```java
import org.cleancoders.web.dto.common.ErrorResponse;
```

`LocalDate` 已有 import。

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl WebApi -am
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java
git commit -m "feat: WebApiRoomPresenter新增pastTimeSlot错误响应"
```

---

### Task 6: 运行全量测试验证

- [ ] **Step 1: 运行全量测试**

```bash
mvn test -pl WebApi -am
```

Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 2: 提交（如有遗漏文件）**

```bash
git add -A
git commit -m "chore: 全量测试通过，按时段区分座位状态功能完成"
```
