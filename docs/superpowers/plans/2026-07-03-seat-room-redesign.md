# 座位与教室重新设计 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用布局模板 + 自动生成座位模型替代现有独立座位管理，座位由教室创建时根据布局自动批量生成。

**Architecture:** 新增 `RoomLayout` 系统预设枚举；`StudyRoom` 用 layout 替换 capacity；`Seat` 的 id 从 String UUID 改为 int 序号，去掉 seatNumber；`Reservation` 新增 roomId 字段，seatId 改为 int；定位座位用 (roomId, seatId) 复合键。

**Tech Stack:** Java 17+, JAX-RS, HK2 DI, JUnit 5, Mockito

## Global Constraints

- 教室创建后布局不可变更
- 座位不可单独新增或删除
- 座位序号 1..N 在教室创建时确定，不可修改
- 系统预设布局枚举值，管理员只能从中选择
- 删除 `ManageSeatsUseCase` 和 `DeleteSeatUseCase`
- 保留 `UpdateSeatUseCase`（AVAILABLE ↔ MAINTENANCE 切换）

---

### Task 1: 新增 RoomLayout 枚举

**Files:**
- Create: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/RoomLayout.java`

**Interfaces:**
- Produces: `RoomLayout` enum with `SMALL(40)`, `MEDIUM(60)`, `LARGE(100)`

- [ ] **Step 1: 创建 RoomLayout 枚举**

```java
package org.cleancoders.seatandroom.domain;

/**
 * 系统预设的教室布局模板。
 * 每种布局定义了固定的座位数量，教室创建后不可更改。
 */
public enum RoomLayout {
    SMALL("小教室", 40),
    MEDIUM("中教室", 60),
    LARGE("大教室", 100);

    private final String displayName;
    private final int seatCount;

    RoomLayout(String displayName, int seatCount) {
        this.displayName = displayName;
        this.seatCount = seatCount;
    }

    public String displayName() {
        return displayName;
    }

    public int seatCount() {
        return seatCount;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd SeatAndRoom && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/RoomLayout.java
git commit -m "feat: 新增 RoomLayout 系统预设布局枚举"
```

---

### Task 2: 修改 StudyRoom — capacity 替换为 layout

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/StudyRoom.java`

**Interfaces:**
- Consumes: `RoomLayout` enum
- Produces: `StudyRoom(String id, String name, String location, RoomLayout layout, RoomStatus status)`

- [ ] **Step 1: 修改 StudyRoom record**

```java
package org.cleancoders.seatandroom.domain;

/**
 * A study room that contains seats.
 */
public record StudyRoom(
        String id,
        String name,
        String location,
        RoomLayout layout,
        RoomStatus status
)
{
}
```

- [ ] **Step 2: 编译验证（预期有引用旧 capacity 的编译错误）**

Run: `cd SeatAndRoom && mvn compile -q`
Expected: 编译错误 — 所有引用 `capacity()` 的地方需要修复（后续任务修复）

- [ ] **Step 3: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/StudyRoom.java
git commit -m "refactor: StudyRoom 用 RoomLayout 替换 capacity"
```

---

### Task 3: 修改 Seat — id 改为 int，去掉 seatNumber

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/Seat.java`

**Interfaces:**
- Produces: `Seat(int id, String roomId, SeatStatus status)` — id 为 int，无 seatNumber

- [ ] **Step 1: 重写 Seat 类**

```java
package org.cleancoders.seatandroom.domain;

/**
 * A seat within a study room.
 * <p>
 * Identified by (roomId, id) composite key — id is a sequential number (1..N)
 * within the room. Mutable class — status transitions are domain operations
 * that validate the current state before allowing the change.
 */
public class Seat {

    private final int id;
    private final String roomId;
    private SeatStatus status;

    public Seat(int id, String roomId, SeatStatus status) {
        this.id = id;
        this.roomId = roomId;
        this.status = status;
    }

    // --- domain business logic ---

    public void reserve() {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "只有可用状态的座位才能预约，当前状态: " + status);
        }
        this.status = SeatStatus.RESERVED;
    }

    public void release() {
        if (status != SeatStatus.RESERVED && status != SeatStatus.OCCUPIED) {
            throw new IllegalStateException(
                    "只有已预约或使用中的座位才能释放，当前状态: " + status);
        }
        this.status = SeatStatus.AVAILABLE;
    }

    public void occupy() {
        if (status != SeatStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约的座位才能签到，当前状态: " + status);
        }
        this.status = SeatStatus.OCCUPIED;
    }

    public void markMaintenance() {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "只有可用状态的座位才能设为维护，当前状态: " + status);
        }
        this.status = SeatStatus.MAINTENANCE;
    }

    public void markAvailable() {
        if (status != SeatStatus.MAINTENANCE) {
            throw new IllegalStateException(
                    "只有维护中的座位才能恢复，当前状态: " + status);
        }
        this.status = SeatStatus.AVAILABLE;
    }

    public void markRemoved() {
        if (status != SeatStatus.AVAILABLE && status != SeatStatus.MAINTENANCE) {
            throw new IllegalStateException(
                    "只有可用或维护中的座位才能删除，当前状态: " + status);
        }
        this.status = SeatStatus.REMOVED;
    }

    // --- getters ---

    public int id() {
        return id;
    }

    public String roomId() {
        return roomId;
    }

    public SeatStatus status() {
        return status;
    }
}
```

- [ ] **Step 2: 编译验证（预期大量编译错误）**

Run: `cd SeatAndRoom && mvn compile -q`
Expected: 编译错误（所有引用 `seatNumber()` 和 `setId()` 的地方需要后续修复）

- [ ] **Step 3: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/domain/Seat.java
git commit -m "refactor: Seat id 改为 int，去掉 seatNumber 和 setId"
```

---

### Task 4: 修改 SeatRepository 接口

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/SeatRepository.java`

**Interfaces:**
- Produces: `findByRoomIdAndSeatId(String, int)` 替代 `findById(String)`, 新增 `deleteByRoomId(String)`

- [ ] **Step 1: 重写 SeatRepository 接口**

```java
package org.cleancoders.seatandroom.outbound;

import org.cleancoders.seatandroom.domain.Seat;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Seat} persistence.
 */
public interface SeatRepository {

    Optional<Seat> findByRoomIdAndSeatId(String roomId, int seatId);

    Seat save(Seat seat);

    List<Seat> findByRoomId(String roomId);

    List<Seat> findAll();

    void deleteByRoomId(String roomId);
}
```

- [ ] **Step 2: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/SeatRepository.java
git commit -m "refactor: SeatRepository 改用 findByRoomIdAndSeatId 复合键，新增 deleteByRoomId"
```

---

### Task 5: 修改 ActiveReservationChecker 接口

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/ActiveReservationChecker.java`

**Interfaces:**
- Produces: `hasActiveForSeat(String roomId, int seatId)` 替代 `hasActiveForSeat(String seatId)`

- [ ] **Step 1: 更新接口签名**

```java
package org.cleancoders.seatandroom.outbound;

/**
 * Narrow port for checking active reservations on a seat before deletion.
 */
public interface ActiveReservationChecker {

    /**
     * Returns {@code true} if the seat identified by (roomId, seatId)
     * has at least one active reservation (RESERVED or CHECKED_IN),
     * meaning it cannot be safely deleted.
     */
    boolean hasActiveForSeat(String roomId, int seatId);
}
```

- [ ] **Step 2: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/outbound/ActiveReservationChecker.java
git commit -m "refactor: ActiveReservationChecker 改用 roomId+seatId 复合键"
```

---

### Task 6: 修改 ManageRoomsUseCase — 创建时自动生成座位

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ManageRoomsUseCase.java`

**Interfaces:**
- Consumes: `RoomLayout`, `Seat(int, String, SeatStatus)`, `SeatRepository.deleteByRoomId`
- Produces: `Request(String token, String name, String location, String layout)`

- [ ] **Step 1: 重写 ManageRoomsUseCase**

```java
package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

import java.util.UUID;

/**
 * UC-06: 管理员管理自习室（创建、更新、删除等）。
 * <p>
 * 当前实现 UC-06：创建自习室。根据所选布局自动生成 N 个座位。
 */
public class ManageRoomsUseCase extends AdminAuthUseCase<ManageRoomsUseCase.Request, ManageRoomsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    RoomRepository roomRepo;

    @Inject
    SeatRepository seatRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        // Check name uniqueness
        var existing = roomRepo.findByName(req.name());
        if (existing.isPresent())
        {
            presenter.roomNameAlreadyExists(req.name());
            return null;
        }

        // Parse layout
        RoomLayout layout;
        try
        {
            layout = RoomLayout.valueOf(req.layout());
        }
        catch (IllegalArgumentException e)
        {
            presenter.invalidLayout(req.layout());
            return null;
        }

        String id = UUID.randomUUID().toString();
        StudyRoom room = new StudyRoom(id, req.name(), req.location(), layout, RoomStatus.OPEN);
        StudyRoom saved = roomRepo.save(room);

        // Auto-generate seats based on layout
        for (int i = 1; i <= layout.seatCount(); i++)
        {
            seatRepo.save(new Seat(i, id, SeatStatus.AVAILABLE));
        }

        presenter.success(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void success(StudyRoom room);

        void roomNameAlreadyExists(String name);

        void invalidLayout(String layout);
    }

    public record Request(String token, String name, String location, String layout)
            implements AuthUseCase.Request
    {
    }

    public record Output(String roomId)
    {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ManageRoomsUseCase.java
git commit -m "refactor: ManageRoomsUseCase 根据布局自动批量生成座位"
```

---

### Task 7: 修改 DeleteRoomUseCase — 级联删除座位

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteRoomUseCase.java`

- [ ] **Step 1: 更新 DeleteRoomUseCase**

Search for the line creating the `closed` StudyRoom record (which currently references `room.capacity()`) and replace with `room.layout()`:

```java
package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

/**
 * UC-06: 删除自习室（管理员）。
 * <p>
 * 将自习室状态标记为 CLOSED（软删除），同时级联删除所有座位。
 */
public class DeleteRoomUseCase extends AdminAuthUseCase<DeleteRoomUseCase.Request, DeleteRoomUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    RoomRepository roomRepo;

    @Inject
    SeatRepository seatRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = roomRepo.findById(req.roomId());
        if (existing.isEmpty())
        {
            presenter.roomNotFound(req.roomId());
            return null;
        }

        StudyRoom room = existing.get();
        if (room.status() == RoomStatus.CLOSED)
        {
            presenter.roomAlreadyClosed(req.roomId());
            return null;
        }

        StudyRoom closed = new StudyRoom(room.id(), room.name(), room.location(), room.layout(), RoomStatus.CLOSED);
        roomRepo.save(closed);

        // Cascade delete seats
        seatRepo.deleteByRoomId(req.roomId());

        presenter.deleteSuccess(req.roomId());
        return new Output(req.roomId());
    }

    public interface Presenter
    {
        void deleteSuccess(String roomId);

        void roomNotFound(String roomId);

        void roomAlreadyClosed(String roomId);
    }

    public record Request(String token, String roomId)
            implements AuthUseCase.Request
    {
    }

    public record Output(String roomId)
    {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteRoomUseCase.java
git commit -m "refactor: DeleteRoomUseCase 级联删除座位，适配新 RoomLayout"
```

---

### Task 8: 修改 UpdateRoomUseCase — 禁止改 layout

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateRoomUseCase.java`

- [ ] **Step 1: 更新 UpdateRoomUseCase**

Request 中去掉 capacity，改为只更新 name 和 location（layout 不可变）：

```java
package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

/**
 * UC-06: 更新自习室信息（管理员）。
 * 仅允许修改名称和位置，布局不可变更。
 */
public class UpdateRoomUseCase extends AdminAuthUseCase<UpdateRoomUseCase.Request, UpdateRoomUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    RoomRepository roomRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = roomRepo.findById(req.roomId());
        if (existing.isEmpty())
        {
            presenter.roomNotFound(req.roomId());
            return null;
        }

        // Check name uniqueness (exclude self)
        var nameConflict = roomRepo.findByName(req.name());
        if (nameConflict.isPresent() && !nameConflict.get().id().equals(req.roomId()))
        {
            presenter.roomNameAlreadyExists(req.name());
            return null;
        }

        StudyRoom old = existing.get();
        StudyRoom updated = new StudyRoom(old.id(), req.name(), req.location(), old.layout(), old.status());
        StudyRoom saved = roomRepo.save(updated);
        presenter.updateSuccess(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void updateSuccess(StudyRoom room);

        void roomNotFound(String roomId);

        void roomNameAlreadyExists(String name);
    }

    public record Request(String token, String roomId, String name, String location)
            implements AuthUseCase.Request
    {
    }

    public record Output(String roomId)
    {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateRoomUseCase.java
git commit -m "refactor: UpdateRoomUseCase 禁止修改布局，只允许改名称和位置"
```

---

### Task 9: 修改 UpdateSeatUseCase — 入参改 (roomId, seatId)

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCase.java`

- [ ] **Step 1: 更新 UpdateSeatUseCase**

```java
package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

/**
 * UC-07: 更新座位状态(管理员)。仅允许 AVAILABLE ↔ MAINTENANCE 切换。
 * 座位通过 (roomId, seatId) 复合键定位。
 */
public class UpdateSeatUseCase extends AdminAuthUseCase<UpdateSeatUseCase.Request, UpdateSeatUseCase.Output>
{
    @Inject
    Presenter presenter;

    @Inject
    SeatRepository seatRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = seatRepo.findByRoomIdAndSeatId(req.roomId(), req.seatId());
        if (existing.isEmpty())
        {
            presenter.seatNotFound(req.roomId(), req.seatId());
            return null;
        }

        Seat seat = existing.get();

        if (req.status() == null)
        {
            presenter.invalidStatus(req.roomId(), req.seatId(), null);
            return null;
        }

        SeatStatus target;
        try
        {
            target = SeatStatus.valueOf(req.status());
        }
        catch (IllegalArgumentException e)
        {
            presenter.invalidStatus(req.roomId(), req.seatId(), req.status());
            return null;
        }

        if (target != SeatStatus.AVAILABLE && target != SeatStatus.MAINTENANCE)
        {
            presenter.invalidStatus(req.roomId(), req.seatId(), req.status());
            return null;
        }

        SeatStatus current = seat.status();
        if (target == SeatStatus.MAINTENANCE)
        {
            if (current != SeatStatus.AVAILABLE)
            {
                presenter.invalidStatusTransition(req.roomId(), req.seatId(), current, target);
                return null;
            }
            seat.markMaintenance();
        }
        else // AVAILABLE
        {
            if (current != SeatStatus.MAINTENANCE)
            {
                presenter.invalidStatusTransition(req.roomId(), req.seatId(), current, target);
                return null;
            }
            seat.markAvailable();
        }

        Seat saved = seatRepo.save(seat);
        presenter.updateSuccess(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void updateSuccess(Seat seat);

        void seatNotFound(String roomId, int seatId);

        void invalidStatusTransition(String roomId, int seatId, SeatStatus current, SeatStatus target);

        void invalidStatus(String roomId, int seatId, String status);
    }

    public record Request(String token, String roomId, int seatId, String status)
            implements AuthUseCase.Request
    {
    }

    public record Output(int seatId)
    {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCase.java
git commit -m "refactor: UpdateSeatUseCase 入参改为 (roomId, seatId) 复合键"
```

---

### Task 10: 删除 ManageSeatsUseCase 和 DeleteSeatUseCase

**Files:**
- Delete: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ManageSeatsUseCase.java`
- Delete: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCase.java`

- [ ] **Step 1: 删除两个文件**

```bash
rm SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ManageSeatsUseCase.java
rm SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCase.java
```

- [ ] **Step 2: Commit**

```bash
git add -u SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ManageSeatsUseCase.java SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCase.java
git commit -m "refactor: 删除 ManageSeatsUseCase 和 DeleteSeatUseCase"
```

---

### Task 11: 修改 ListSeatsUseCase — 适配新 Seat 模型

**Files:**
- Modify: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/ListSeatsUseCase.java`

- [ ] **Step 1: 无需代码变更（只依赖 findByRoomId，接口未变），确认即可**

ListSeatsUseCase 只使用了 `seatRepo.findByRoomId()` 和 `roomRepo.findById()`，这两个方法签名未变。Seat 的 id 类型变化不影响此用例的编译。无需修改。

- [ ] **Step 2: Commit**

```bash
# No changes needed — skip commit
```

---

### Task 12: 修改 Reservation 领域模型 — 新增 roomId, seatId 改 int

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/domain/Reservation.java`

**Interfaces:**
- Produces: `Reservation(String id, String userId, String roomId, int seatId, String timeSlotId, LocalDate date)`
- `seatId()` 返回 int
- 新增 `roomId()` 返回 String

- [ ] **Step 1: 更新 Reservation 类**

```java
package org.cleancoders.reservation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A seat reservation made by a student for a specific seat, date, and time slot.
 * <p>
 * The seat is identified by the composite key (roomId, seatId).
 */
public class Reservation {

    private String id;
    private final String userId;
    private final String roomId;
    private final int seatId;
    private final String timeSlotId;
    private final LocalDate date;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    public Reservation(String id, String userId, String roomId, int seatId, String timeSlotId, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.seatId = seatId;
        this.timeSlotId = timeSlotId;
        this.date = date;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = LocalDateTime.now();
    }

    // --- domain business logic (unchanged) ---

    public void cancel() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能取消，当前状态: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void checkIn() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能签到，当前状态: " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
        this.checkInAt = LocalDateTime.now();
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "只有已签到状态才能退座，当前状态: " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
        this.checkOutAt = LocalDateTime.now();
    }

    public void expire() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能标记为过期，当前状态: " + status);
        }
        this.status = ReservationStatus.EXPIRED;
    }

    // --- getters ---

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String roomId() {
        return roomId;
    }

    public int seatId() {
        return seatId;
    }

    public String timeSlotId() {
        return timeSlotId;
    }

    public LocalDate date() {
        return date;
    }

    public ReservationStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime checkInAt() {
        return checkInAt;
    }

    public LocalDateTime checkOutAt() {
        return checkOutAt;
    }

    // --- setters (for infrastructure use) ---

    public void setId(String id) {
        this.id = id;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/domain/Reservation.java
git commit -m "refactor: Reservation 新增 roomId，seatId 改为 int"
```

---

### Task 13: 修改 ReservationRepository 接口

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/outbound/ReservationRepository.java`

- [ ] **Step 1: 更新方法签名**

`findBySeatIdAndDateAndTimeSlotIdAndStatusIn` 和 `findBySeatIdAndStatusIn` 的 seatId 参数改为 `(String roomId, int seatId)`：

```java
package org.cleancoders.reservation.outbound;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(String id);

    Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
            String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses);

    Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
            String roomId, int seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses);

    List<Reservation> findByUserId(String userId);

    List<Reservation> findAll();

    List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> statuses);
}
```

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/outbound/ReservationRepository.java
git commit -m "refactor: ReservationRepository 查询方法改用 roomId+seatId 复合键"
```

---

### Task 14: 修改 ReserveUseCase — 入参改为 (roomId, seatId)

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/usecase/ReserveUseCase.java`

- [ ] **Step 1: 更新 ReserveUseCase**

`Request` 改为 `(String token, String roomId, int seatId, String timeSlotId, LocalDate date)`，所有 `seatRepo.findById()` 改为 `seatRepo.findByRoomIdAndSeatId()`：

```java
package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth.usecase.StudentAuthUseCase;

import java.time.LocalDate;
import java.util.Set;

/**
 * UC-08: 创建预约。
 * <p>
 * 学生选择座位 (roomId + seatId) + 时段 + 日期，通过冲突检测后创建预约记录。
 */
public class ReserveUseCase extends StudentAuthUseCase<ReserveUseCase.Request, ReserveUseCase.Output>
{

    private static final Set<ReservationStatus> ACTIVE_STATUSES =
            Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN);
    @Inject
    Presenter presenter;
    @Inject
    ReservationRepository reservationRepo;
    @Inject
    SeatRepository seatRepo;
    @Inject
    TimeSlotRepository timeSlotRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        // 1. Validate time slot exists
        var timeSlot = timeSlotRepo.findById(req.timeSlotId());
        if (timeSlot.isEmpty())
        {
            presenter.timeSlotNotFound(req.timeSlotId());
            return null;
        }

        // 2. Validate seat exists
        var seatOpt = seatRepo.findByRoomIdAndSeatId(req.roomId(), req.seatId());
        if (seatOpt.isEmpty())
        {
            presenter.seatNotFound(req.roomId(), req.seatId());
            return null;
        }

        var seat = seatOpt.get();

        // 3. Check seat is not in MAINTENANCE
        if (seat.status() == SeatStatus.MAINTENANCE)
        {
            presenter.seatNotAvailable(req.roomId(), req.seatId(), timeSlot.get().label());
            return null;
        }

        // 4. Check user doesn't already have a reservation for same date+timeslot
        var existingUserReservation = reservationRepo.findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                user.id(), req.date(), req.timeSlotId(), ACTIVE_STATUSES);
        if (existingUserReservation.isPresent())
        {
            presenter.duplicateReservation(existingUserReservation.get().id());
            return null;
        }

        // 5. Check seat not already reserved for same date+timeslot
        var existingSeatReservation = reservationRepo.findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                req.roomId(), req.seatId(), req.date(), req.timeSlotId(), ACTIVE_STATUSES);
        if (existingSeatReservation.isPresent())
        {
            presenter.seatNotAvailable(req.roomId(), req.seatId(), timeSlot.get().label());
            return null;
        }

        // 6. Create and save the reservation
        Reservation reservation = new Reservation(null, user.id(), req.roomId(), req.seatId(), req.timeSlotId(), req.date());
        Reservation saved = reservationRepo.save(reservation);

        // 7. Present success
        presenter.success(saved.id(), String.valueOf(req.seatId()), timeSlot.get().label());
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void success(String reservationId, String seatNumber, String timeSlot);

        void seatNotAvailable(String roomId, int seatId, String timeSlot);

        void duplicateReservation(String existingId);

        void timeSlotNotFound(String timeSlotId);

        void seatNotFound(String roomId, int seatId);
    }

    public record Request(String token, String roomId, int seatId, String timeSlotId, LocalDate date)
            implements AuthUseCase.Request
    {
    }

    public record Output(String reservationId)
    {
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/usecase/ReserveUseCase.java
git commit -m "refactor: ReserveUseCase 入参改为 (roomId, seatId) 复合键"
```

---

### Task 15: 修改 CheckInUseCase — 适配新 Reservation 和 Seat 模型

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/usecase/CheckInUseCase.java`

- [ ] **Step 1: 更新 seatRepo 查询和 seatNumber 引用**

将 `seatRepo.findById(reservation.seatId())` 改为 `seatRepo.findByRoomIdAndSeatId(reservation.roomId(), reservation.seatId())`，将 `Seat::seatNumber` 改为 `s -> String.valueOf(s.id())`：

关键变更在第 119-120 行附近：
```java
// 7. Look up seat for response
var seatOpt = seatRepo.findByRoomIdAndSeatId(reservation.roomId(), reservation.seatId());
String seatNumber = seatOpt.map(s -> String.valueOf(s.id())).orElse("未知");
```

完整文件保持不变，仅改上述两处。

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/usecase/CheckInUseCase.java
git commit -m "refactor: CheckInUseCase 适配新 Reservation 和 Seat 模型"
```

---

### Task 16: 修改 CheckOutUseCase — 适配新模型

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/usecase/CheckOutUseCase.java`

- [ ] **Step 1: 更新 seatRepo 查询**

将第 75 行 `seatRepo.findById(reservation.seatId())` 改为 `seatRepo.findByRoomIdAndSeatId(reservation.roomId(), reservation.seatId())`，`seat.seatNumber()` 改为 `String.valueOf(seat.id())`：

```java
// 5. Release the seat
var seatOpt = seatRepo.findByRoomIdAndSeatId(reservation.roomId(), reservation.seatId());
String seatNumber = "未知";
if (seatOpt.isPresent())
{
    Seat seat = seatOpt.get();
    seat.release();
    seatRepo.save(seat);
    seatNumber = String.valueOf(seat.id());
}
```

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/usecase/CheckOutUseCase.java
git commit -m "refactor: CheckOutUseCase 适配新 Reservation 和 Seat 模型"
```

---

### Task 17: 修改 CancelReservationUseCase — 适配新模型

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/usecase/CancelReservationUseCase.java`

- [ ] **Step 1: 更新 seatRepo 查询**

将第 90 行 `seatRepo.findById(reservation.seatId())` 改为 `seatRepo.findByRoomIdAndSeatId(reservation.roomId(), reservation.seatId())`，`Seat::seatNumber` 改为 `s -> String.valueOf(s.id())`。

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/usecase/CancelReservationUseCase.java
git commit -m "refactor: CancelReservationUseCase 适配新 Reservation 和 Seat 模型"
```

---

### Task 18: 修改 ListMyReservationsUseCase — 适配新模型

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/usecase/ListMyReservationsUseCase.java`

- [ ] **Step 1: 更新 ReservationItem 和 seatRepo 查询**

`ReservationItem` 的 `seatId` 改为 `int`，`seatNumber` 改为 `int seatId` 的字符串形式：

```java
// 更新 ReservationItem record
public record ReservationItem(
        String reservationId,
        String roomId,
        int seatId,
        String timeSlotId,
        String timeSlotLabel,
        LocalDate date,
        String status,
        LocalDateTime createdAt
) {
}

// doExecute 中的映射改为:
List<ReservationItem> items = reservations.stream()
        .map(r -> {
            String timeSlotLabel = timeSlotRepo.findById(r.timeSlotId())
                    .map(TimeSlot::label).orElse("未知");

            return new ReservationItem(
                    r.id(), r.roomId(), r.seatId(),
                    r.timeSlotId(), timeSlotLabel,
                    r.date(), r.status().name(), r.createdAt()
            );
        })
        .toList();
```

注意：去掉 `seatRepo.findById()` 调用，因为 seatNumber 不再需要（用 seatId 整数表示座位即可）。

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/usecase/ListMyReservationsUseCase.java
git commit -m "refactor: ListMyReservationsUseCase 适配新模型，去 seatNumber"
```

---

### Task 19: 修改 ManageReservationsUseCase — 适配新模型

**Files:**
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/usecase/ManageReservationsUseCase.java`

- [ ] **Step 1: 更新 ReservationItem 和 seatRepo 查询**

```java
// ReservationItem: seatId 改 int，去掉 seatNumber
public record ReservationItem(
        String reservationId,
        String userId,
        String username,
        String roomId,
        int seatId,
        String timeSlotId,
        String timeSlotLabel,
        LocalDate date,
        String status,
        LocalDateTime createdAt,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt
) {
}

// doExecute 中的映射:
List<ReservationItem> items = reservations.stream()
        .map(r -> {
            String username = userRepo.findById(r.userId())
                    .map(User::username).orElse("未知");
            String timeSlotLabel = timeSlotRepo.findById(r.timeSlotId())
                    .map(TimeSlot::label).orElse("未知");

            return new ReservationItem(
                    r.id(), r.userId(), username,
                    r.roomId(), r.seatId(),
                    r.timeSlotId(), timeSlotLabel,
                    r.date(), r.status().name(),
                    r.createdAt(), r.checkInAt(), r.checkOutAt()
            );
        })
        .toList();
```

去掉 `seatRepo.findById()` 调用。

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/main/java/org/cleancoders/reservation/usecase/ManageReservationsUseCase.java
git commit -m "refactor: ManageReservationsUseCase 适配新模型"
```

---

### Task 20: 修改 InMemorySeatRepo — 适配新接口

**Files:**
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemorySeatRepo.java`

- [ ] **Step 1: 重写 InMemorySeatRepo**

由于 Seat.id 从 String UUID 变为 int，存储结构需要调整。使用复合键 `(roomId, seatId)` 作为 map key：

```java
package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemorySeatRepo implements SeatRepository {

    private final Map<String, Seat> store = new ConcurrentHashMap<>();

    private static String key(String roomId, int seatId) {
        return roomId + ":" + seatId;
    }

    public InMemorySeatRepo() {
        // Pre-seed: 8 seats in room-1
        String room1 = "room-1";
        for (int i = 1; i <= 8; i++) {
            store.put(key(room1, i), new Seat(i, room1, SeatStatus.AVAILABLE));
        }
        // 4 seats in room-2
        String room2 = "room-2";
        for (int i = 1; i <= 4; i++) {
            store.put(key(room2, i), new Seat(i, room2, SeatStatus.AVAILABLE));
        }
    }

    @Override
    public Optional<Seat> findByRoomIdAndSeatId(String roomId, int seatId) {
        return Optional.ofNullable(store.get(key(roomId, seatId)));
    }

    @Override
    public Seat save(Seat seat) {
        store.put(key(seat.roomId(), seat.id()), seat);
        return seat;
    }

    @Override
    public List<Seat> findByRoomId(String roomId) {
        return store.values().stream()
                .filter(s -> s.roomId().equals(roomId))
                .toList();
    }

    @Override
    public List<Seat> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void deleteByRoomId(String roomId) {
        store.entrySet().removeIf(e -> e.getValue().roomId().equals(roomId));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemorySeatRepo.java
git commit -m "refactor: InMemorySeatRepo 适配新 SeatRepository 接口"
```

---

### Task 21: 修改 TestDataSeatRepo — 适配新 Seat 模型

**Files:**
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/testdata/TestDataSeatRepo.java`

- [ ] **Step 1: 重写 TestDataSeatRepo**

```java
package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;

/**
 * {@link InMemorySeatRepo} pre-seeded with extra test seats.
 * Inherits the base seats (room-1: 8 seats, room-2: 4 seats) from parent
 * and adds additional seats for room-3, room-4, and room-5.
 */
public class TestDataSeatRepo extends InMemorySeatRepo {

    public TestDataSeatRepo() {
        // Inherits room-1 (8 seats) and room-2 (4 seats) from parent

        // room-3: 6 seats
        save(new Seat(1, "room-3", SeatStatus.AVAILABLE));
        save(new Seat(2, "room-3", SeatStatus.AVAILABLE));
        save(new Seat(3, "room-3", SeatStatus.RESERVED));
        save(new Seat(4, "room-3", SeatStatus.MAINTENANCE));
        save(new Seat(5, "room-3", SeatStatus.OCCUPIED));
        save(new Seat(6, "room-3", SeatStatus.AVAILABLE));

        // room-4: 4 seats
        save(new Seat(1, "room-4", SeatStatus.AVAILABLE));
        save(new Seat(2, "room-4", SeatStatus.AVAILABLE));
        save(new Seat(3, "room-4", SeatStatus.MAINTENANCE));
        save(new Seat(4, "room-4", SeatStatus.AVAILABLE));

        // room-5: 6 seats
        save(new Seat(1, "room-5", SeatStatus.AVAILABLE));
        save(new Seat(2, "room-5", SeatStatus.AVAILABLE));
        save(new Seat(3, "room-5", SeatStatus.OCCUPIED));
        save(new Seat(4, "room-5", SeatStatus.AVAILABLE));
        save(new Seat(5, "room-5", SeatStatus.RESERVED));
        save(new Seat(6, "room-5", SeatStatus.AVAILABLE));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/testdata/TestDataSeatRepo.java
git commit -m "refactor: TestDataSeatRepo 适配新 Seat 模型"
```

---

### Task 22: 修改 TestDataRoomRepo — 适配新 StudyRoom

**Files:**
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/testdata/TestDataRoomRepo.java`

- [ ] **Step 1: 更新 StudyRoom 构造**

```java
package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.infrastructure.persistence.InMemoryRoomRepo;
import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;

public class TestDataRoomRepo extends InMemoryRoomRepo {

    public TestDataRoomRepo() {
        save(new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN));
        save(new StudyRoom("room-2", "自习室B", "图书馆二楼", RoomLayout.SMALL, RoomStatus.OPEN));
        save(new StudyRoom("room-3", "自习室C", "教学楼三楼", RoomLayout.MEDIUM, RoomStatus.MAINTENANCE));
        save(new StudyRoom("room-4", "自习室D", "图书馆三楼", RoomLayout.MEDIUM, RoomStatus.CLOSED));
        save(new StudyRoom("room-5", "自习室E", "综合楼一楼", RoomLayout.LARGE, RoomStatus.OPEN));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/testdata/TestDataRoomRepo.java
git commit -m "refactor: TestDataRoomRepo 适配新 StudyRoom（RoomLayout）"
```

---

### Task 23: 修改 InMemoryReservationRepo — 适配新接口

**Files:**
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryReservationRepo.java`

- [ ] **Step 1: 更新方法签名**

`findBySeatIdAndDateAndTimeSlotIdAndStatusIn` 和 `findBySeatIdAndStatusIn` 的方法签名改为接受 `roomId` 和 `seatId`：

```java
@Override
public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
        String roomId, int seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
    return store.values().stream()
            .filter(r -> r.roomId().equals(roomId))
            .filter(r -> r.seatId() == seatId)
            .filter(r -> r.date().equals(date))
            .filter(r -> r.timeSlotId().equals(timeSlotId))
            .filter(r -> statuses.contains(r.status()))
            .findFirst();
}

@Override
public List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> statuses) {
    return store.values().stream()
            .filter(r -> r.roomId().equals(roomId))
            .filter(r -> r.seatId() == seatId)
            .filter(r -> statuses.contains(r.status()))
            .toList();
}
```

- [ ] **Step 2: Commit**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryReservationRepo.java
git commit -m "refactor: InMemoryReservationRepo 适配新 ReservationRepository 接口"
```

---

### Task 24: 修改 ReservationBasedActiveReservationChecker — 适配新接口

**Files:**
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationChecker.java`

- [ ] **Step 1: 更新方法**

```java
@Override
public boolean hasActiveForSeat(String roomId, int seatId) {
    return !reservationRepo.findBySeatIdAndStatusIn(roomId, seatId,
            Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN)).isEmpty();
}
```

- [ ] **Step 2: Commit**

```bash
git add Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationChecker.java
git commit -m "refactor: ReservationBasedActiveReservationChecker 适配新接口"
```

---

### Task 25: 修改 DTO — CreateRoomRequest, RoomResponse, ReserveInput, SeatResponse, UpdateSeatRequest

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/dto/admin/CreateRoomRequest.java`
- Modify: `WebApi/src/main/java/org/cleancoders/web/dto/room/RoomResponse.java`
- Modify: `WebApi/src/main/java/org/cleancoders/web/dto/reservation/ReserveInput.java`
- Modify: `WebApi/src/main/java/org/cleancoders/web/dto/seat/SeatResponse.java`
- Modify: `WebApi/src/main/java/org/cleancoders/web/dto/admin/UpdateSeatRequest.java`
- Delete: `WebApi/src/main/java/org/cleancoders/web/dto/admin/CreateSeatRequest.java`

- [ ] **Step 1: 修改 CreateRoomRequest**

```java
package org.cleancoders.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建自习室请求")
public record CreateRoomRequest(
        @Schema(description = "自习室名称", example = "自习室F")
        String name,
        @Schema(description = "位置", example = "综合楼二楼")
        String location,
        @Schema(description = "布局类型", example = "SMALL",
                allowableValues = {"SMALL", "MEDIUM", "LARGE"})
        String layout
)
{
}
```

- [ ] **Step 2: 修改 RoomResponse**

```java
package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;

@Schema(description = "自习室信息")
public record RoomResponse(
        @Schema(description = "自习室ID", example = "r1")
        String id,
        @Schema(description = "名称", example = "A 自习室")
        String name,
        @Schema(description = "位置", example = "1号楼2层")
        String location,
        @Schema(description = "布局类型", example = "SMALL")
        String layout,
        @Schema(description = "座位数量", example = "40")
        int seatCount,
        @Schema(description = "状态", example = "OPEN", allowableValues = {"OPEN", "CLOSED", "MAINTENANCE"})
        RoomStatus status
)
{
}
```

- [ ] **Step 3: 修改 ReserveInput**

```java
package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预约创建请求")
public record ReserveInput(
        @Schema(description = "教室 ID", example = "room-1")
        String roomId,

        @Schema(description = "座位序号 (1-N)", example = "5")
        int seatId,

        @Schema(description = "时段 ID", example = "ts-1")
        String timeSlotId,

        @Schema(description = "预约日期 (ISO 格式: YYYY-MM-DD)", example = "2026-07-02")
        String date
) {
}
```

- [ ] **Step 4: 修改 SeatResponse**

```java
package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.seatandroom.domain.SeatStatus;

@Schema(description = "座位信息")
public record SeatResponse(
        @Schema(description = "座位序号 (教室内 1-N)", example = "5")
        int id,
        @Schema(description = "所属教室ID", example = "room-1")
        String roomId,
        @Schema(description = "座位状态",
                allowableValues = {"AVAILABLE", "RESERVED", "OCCUPIED", "MAINTENANCE"})
        SeatStatus status
)
{
}
```

- [ ] **Step 5: 修改 UpdateSeatRequest — 新增 roomId**

```java
package org.cleancoders.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新座位状态请求")
public record UpdateSeatRequest(
        @Schema(description = "教室ID", example = "room-1")
        String roomId,
        @Schema(description = "目标状态", example = "MAINTENANCE",
                allowableValues = {"AVAILABLE", "MAINTENANCE"})
        String status
)
{
}
```

- [ ] **Step 6: 删除 CreateSeatRequest**

```bash
rm WebApi/src/main/java/org/cleancoders/web/dto/admin/CreateSeatRequest.java
```

- [ ] **Step 7: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/dto/admin/CreateRoomRequest.java
git add WebApi/src/main/java/org/cleancoders/web/dto/room/RoomResponse.java
git add WebApi/src/main/java/org/cleancoders/web/dto/reservation/ReserveInput.java
git add WebApi/src/main/java/org/cleancoders/web/dto/seat/SeatResponse.java
git add WebApi/src/main/java/org/cleancoders/web/dto/admin/UpdateSeatRequest.java
git add -u WebApi/src/main/java/org/cleancoders/web/dto/admin/CreateSeatRequest.java
git commit -m "refactor: 更新所有 DTO 适配新座位/教室模型，删除 CreateSeatRequest"
```

---

### Task 26: 修改 WebApiRoomPresenter — 适配新模型

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java`

- [ ] **Step 1: 更新所有涉及 RoomResponse, SeatResponse 的构造，以及 Presenter 接口方法签名变更**

关键变更：
1. `RoomResponse` 构造：`r.capacity()` → `r.layout().name()`, 新增 `r.layout().seatCount()`
2. `SeatResponse` 构造：`new SeatResponse(s.id(), s.seatNumber(), s.status())` → `new SeatResponse(s.id(), s.roomId(), s.status())`
3. `ManageSeatsUseCase.Presenter` 和 `DeleteSeatUseCase.Presenter` 移除
4. `UpdateSeatUseCase.Presenter` 方法签名变更：`seatNotFound(String seatId)` → `seatNotFound(String roomId, int seatId)`
5. `invalidStatus` / `invalidStatusTransition` 同理加 roomId + int seatId

完整代码：

```java
package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.*;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.web.dto.room.RoomListResponse;
import org.cleancoders.web.dto.room.RoomResponse;
import org.cleancoders.web.dto.seat.SeatListResponse;
import org.cleancoders.web.dto.seat.SeatResponse;

import java.util.List;
import java.util.Map;

@Singleton
public class WebApiRoomPresenter extends WebApiPresenter implements
        ListRoomsUseCase.Presenter,
        ListSeatsUseCase.Presenter,
        ManageRoomsUseCase.Presenter,
        UpdateRoomUseCase.Presenter,
        DeleteRoomUseCase.Presenter,
        UpdateSeatUseCase.Presenter
{

    @Override
    public void presentRooms(List<StudyRoom> rooms)
    {
        List<RoomResponse> dtos = rooms.stream()
                .map(r -> new RoomResponse(r.id(), r.name(), r.location(),
                        r.layout().name(), r.layout().seatCount(), r.status()))
                .toList();
        responseContext.set(Response.ok(new RoomListResponse(dtos)).build());
    }

    @Override
    public void presentSeats(StudyRoom room, List<Seat> seats)
    {
        List<SeatResponse> dtos = seats.stream()
                .map(s -> new SeatResponse(s.id(), s.roomId(), s.status()))
                .toList();
        responseContext.set(Response.ok(new SeatListResponse(room.id(), room.name(), dtos)).build());
    }

    @Override
    public void roomNotFound(String roomId)
    {
        responseContext.set(Response.status(404).entity(Map.of(
                "error", "自习室不存在",
                "roomId", roomId
        )).build());
    }

    @Override
    public void success(StudyRoom room)
    {
        responseContext.set(Response.status(201).entity(
                new RoomResponse(room.id(), room.name(), room.location(),
                        room.layout().name(), room.layout().seatCount(), room.status())
        ).build());
    }

    @Override
    public void updateSuccess(StudyRoom room)
    {
        responseContext.set(Response.ok(
                new RoomResponse(room.id(), room.name(), room.location(),
                        room.layout().name(), room.layout().seatCount(), room.status())
        ).build());
    }

    @Override
    public void roomNameAlreadyExists(String name)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "自习室名称已存在",
                "name", name
        )).build());
    }

    @Override
    public void invalidLayout(String layout)
    {
        responseContext.set(Response.status(400).entity(Map.of(
                "error", "无效的布局类型",
                "layout", layout,
                "validValues", new String[]{"SMALL", "MEDIUM", "LARGE"}
        )).build());
    }

    @Override
    public void deleteSuccess(String roomId)
    {
        responseContext.set(Response.status(200).entity(Map.of(
                "message", "自习室已删除",
                "roomId", roomId
        )).build());
    }

    @Override
    public void roomAlreadyClosed(String roomId)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "自习室已处于关闭状态",
                "roomId", roomId
        )).build());
    }

    // --- UpdateSeatUseCase (UC-07 update seat status) ---

    @Override
    public void updateSuccess(Seat seat)
    {
        responseContext.set(Response.ok(
                new SeatResponse(seat.id(), seat.roomId(), seat.status())
        ).build());
    }

    @Override
    public void seatNotFound(String roomId, int seatId)
    {
        responseContext.set(Response.status(404).entity(Map.of(
                "error", "座位不存在",
                "roomId", roomId,
                "seatId", seatId
        )).build());
    }

    @Override
    public void invalidStatusTransition(String roomId, int seatId, SeatStatus current, SeatStatus target)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "非法状态转换",
                "roomId", roomId,
                "seatId", seatId,
                "currentStatus", current.name(),
                "targetStatus", target.name()
        )).build());
    }

    @Override
    public void invalidStatus(String roomId, int seatId, String status)
    {
        responseContext.set(Response.status(400).entity(Map.of(
                "error", "非法座位状态",
                "roomId", roomId,
                "seatId", seatId,
                "status", status == null ? "null" : status
        )).build());
    }
}
```

注意：移除了 `ManageSeatsUseCase.Presenter` 和 `DeleteSeatUseCase.Presenter` 的实现。

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java
git commit -m "refactor: WebApiRoomPresenter 适配新模型，移除已删除用例的 Presenter"
```

---

### Task 27: 修改 WebApiReservationPresenter — 适配新 Presenter 接口

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiReservationPresenter.java`

- [ ] **Step 1: 更新 `seatNotAvailable` 和 `seatNotFound` 方法签名**

```java
@Override
public void seatNotAvailable(String roomId, int seatId, String timeSlot)
{
    responseContext.set(Response.status(409).entity(new SeatConflictResponse(
            "座位已被预约", roomId + ":" + seatId, timeSlot)).build());
}

@Override
public void seatNotFound(String roomId, int seatId)
{
    responseContext.set(Response.status(404).entity(new SeatNotFoundResponse(
            "座位不存在", roomId + ":" + seatId)).build());
}
```

同时更新 `ListMyReservationsUseCase.Presenter` 的 `presentReservations` 方法适配新的 `ReservationItem`（去 seatNumber，新 roomId + int seatId）：

```java
@Override
public void presentReservations(List<ListMyReservationsUseCase.ReservationItem> items)
{
    var list = items.stream()
            .map(item -> Map.of(
                    "reservationId", item.reservationId(),
                    "roomId", item.roomId(),
                    "seatId", item.seatId(),
                    "timeSlotId", item.timeSlotId(),
                    "timeSlotLabel", item.timeSlotLabel(),
                    "date", item.date().toString(),
                    "status", item.status(),
                    "createdAt", item.createdAt().toString()
            ))
            .toList();

    responseContext.set(Response.ok(Map.of("reservations", list)).build());
}
```

同样更新 `ManageReservationsUseCase.Presenter`：

```java
@Override
public void presentAllReservations(List<ManageReservationsUseCase.ReservationItem> items)
{
    var list = items.stream()
            .map(item -> {
                var m = new LinkedHashMap<String, Object>();
                m.put("reservationId", item.reservationId());
                m.put("userId", item.userId());
                m.put("username", item.username());
                m.put("roomId", item.roomId());
                m.put("seatId", item.seatId());
                m.put("timeSlotId", item.timeSlotId());
                m.put("timeSlotLabel", item.timeSlotLabel());
                m.put("date", item.date().toString());
                m.put("status", item.status());
                m.put("createdAt", item.createdAt() != null ? item.createdAt().toString() : null);
                m.put("checkInAt", item.checkInAt() != null ? item.checkInAt().toString() : null);
                m.put("checkOutAt", item.checkOutAt() != null ? item.checkOutAt().toString() : null);
                return m;
            })
            .toList();

    responseContext.set(Response.ok(Map.of("reservations", list)).build());
}
```

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/presenter/WebApiReservationPresenter.java
git commit -m "refactor: WebApiReservationPresenter 适配新 Presenter 接口"
```

---

### Task 28: 修改 AdminResource — 适配新 API

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java`

- [ ] **Step 1: 更新 AdminResource**

移除 `ManageSeatsUseCase` 和 `DeleteSeatUseCase` 的注入和使用，更新 `createRoom`、`updateRoom`、`updateSeat` 方法：

```java
// 移除这些注入
// @Inject ManageSeatsUseCase manageSeatsUseCase;
// @Inject DeleteSeatUseCase deleteSeatUseCase;

// 移除 createSeat() 和 deleteSeat() 方法

// 修改 createRoom 方法:
public Response createRoom(@CookieParam("Authorization") String authCookie, CreateRoomRequest input)
{
    manageRoomsUseCase.execute(new ManageRoomsUseCase.Request(
            authCookie, input.name(), input.location(), input.layout()));
    return responseContext.get();
}

// 修改 updateRoom 方法（Request 不再含 capacity，新增 name 和 location）:
public Response updateRoom(
        @CookieParam("Authorization") String authCookie,
        @PathParam("id") String roomId,
        CreateRoomRequest input)
{
    updateRoomUseCase.execute(new UpdateRoomUseCase.Request(
            authCookie, roomId, input.name(), input.location()));
    return responseContext.get();
}

// 修改 updateSeat 方法:
@PUT
@Path("/rooms/{roomId}/seats/{seatId}")
@Operation(summary = "更新座位状态 (UC-07)", description = "管理员切换座位状态。通过教室ID+座位序号定位。")
@ApiResponses({...})
public Response updateSeat(
        @CookieParam("Authorization") String authCookie,
        @PathParam("roomId") String roomId,
        @PathParam("seatId") int seatId,
        UpdateSeatRequest input)
{
    updateSeatUseCase.execute(new UpdateSeatUseCase.Request(
            authCookie, roomId, seatId, input.status()));
    return responseContext.get();
}
```

删除 `createSeat()` 和 `deleteSeat()` 方法及相关 import。

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java
git commit -m "refactor: AdminResource 移除座位增删接口，适配新 API"
```

---

### Task 29: 修改 ReservationResource — 适配 ReserveInput

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/resource/ReservationResource.java`

- [ ] **Step 1: 更新 reserve 方法**

```java
public Response reserve(@CookieParam("Authorization") String authCookie, ReserveInput input) {
    LocalDate date;
    try {
        date = LocalDate.parse(input.date());
    } catch (Exception e) {
        return Response.status(400).entity(new InvalidDateResponse(
                "日期格式不合法，请使用 YYYY-MM-DD 格式", input.date())).build();
    }

    reserveUseCase.execute(new ReserveUseCase.Request(
            authCookie, input.roomId(), input.seatId(), input.timeSlotId(), date));
    return responseContext.get();
}
```

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/resource/ReservationResource.java
git commit -m "refactor: ReservationResource 适配新 ReserveInput (roomId+seatId)"
```

---

### Task 30: 修改 SeatAndRoomBinder — 移除已删除用例的绑定

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java`

- [ ] **Step 1: 移除 ManageSeatsUseCase 和 DeleteSeatUseCase 的绑定**

```java
// 移除这两行：
// bind(ManageSeatsUseCase.class).to(ManageSeatsUseCase.class);
// bind(DeleteSeatUseCase.class).to(DeleteSeatUseCase.class);

// 移除 Presenter 绑定中的：
// .to(ManageSeatsUseCase.Presenter.class)
// .to(DeleteSeatUseCase.Presenter.class)
```

更新后的 configure():

```java
@Override
protected void configure()
{
    // === UseCases ===
    bind(ListRoomsUseCase.class).to(ListRoomsUseCase.class);
    bind(ListSeatsUseCase.class).to(ListSeatsUseCase.class);
    bind(ManageRoomsUseCase.class).to(ManageRoomsUseCase.class);
    bind(UpdateRoomUseCase.class).to(UpdateRoomUseCase.class);
    bind(DeleteRoomUseCase.class).to(DeleteRoomUseCase.class);
    bind(UpdateSeatUseCase.class).to(UpdateSeatUseCase.class);

    // === Presenters ===
    bind(WebApiRoomPresenter.class)
            .to(ListRoomsUseCase.Presenter.class)
            .to(ListSeatsUseCase.Presenter.class)
            .to(ManageRoomsUseCase.Presenter.class)
            .to(UpdateRoomUseCase.Presenter.class)
            .to(DeleteRoomUseCase.Presenter.class)
            .to(UpdateSeatUseCase.Presenter.class)
            .in(Singleton.class);

    // === Infrastructure ===
    bind(TestDataSeatRepo.class).to(SeatRepository.class).in(Singleton.class);
    bind(TestDataTimeSlotRepo.class).to(TimeSlotRepository.class).in(Singleton.class);
    bind(TestDataRoomRepo.class).to(RoomRepository.class).in(Singleton.class);
    bind(ReservationBasedActiveReservationChecker.class).to(ActiveReservationChecker.class).in(Singleton.class);
}
```

- [ ] **Step 2: Commit**

```bash
git add WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java
git commit -m "refactor: SeatAndRoomBinder 移除已删除用例的绑定"
```

---

### Task 31: 修改测试基础设施 StubSeatRepo

**Files:**
- Modify: `SeatAndRoom_Test_Infrastructure/src/main/java/org/cleancoders/seatandroom_test_infrastructure/StubSeatRepo.java`
- Modify: `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/test/infrastructure/StubSeatRepo.java`

- [ ] **Step 1: 更新两个 StubSeatRepo 实现新接口**

```java
package org.cleancoders.seatandroom_test_infrastructure;

import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.outbound.SeatRepository;

import java.util.List;
import java.util.Optional;

public class StubSeatRepo implements SeatRepository
{
    private final java.util.Map<String, Seat> seats = new java.util.LinkedHashMap<>();

    private static String key(String roomId, int seatId) {
        return roomId + ":" + seatId;
    }

    public void addSeat(Seat seat)
    {
        seats.put(key(seat.roomId(), seat.id()), seat);
    }

    @Override
    public Optional<Seat> findByRoomIdAndSeatId(String roomId, int seatId)
    {
        return Optional.ofNullable(seats.get(key(roomId, seatId)));
    }

    @Override
    public Seat save(Seat seat)
    {
        seats.put(key(seat.roomId(), seat.id()), seat);
        return seat;
    }

    @Override
    public List<Seat> findByRoomId(String roomId)
    {
        return seats.values().stream()
                .filter(s -> s.roomId().equals(roomId))
                .toList();
    }

    @Override
    public List<Seat> findAll()
    {
        return List.copyOf(seats.values());
    }

    @Override
    public void deleteByRoomId(String roomId)
    {
        seats.entrySet().removeIf(e -> e.getValue().roomId().equals(roomId));
    }
}
```

- [ ] **Step 2: 同步更新 SeatAndRoom 测试目录下的 StubSeatRepo（同内容）**

- [ ] **Step 3: Commit**

```bash
git add SeatAndRoom_Test_Infrastructure/src/main/java/org/cleancoders/seatandroom_test_infrastructure/StubSeatRepo.java
git add SeatAndRoom/src/test/java/org/cleancoders/seatandroom/test/infrastructure/StubSeatRepo.java
git commit -m "refactor: StubSeatRepo 适配新 SeatRepository 接口"
```

---

### Task 32: 修改测试基础设施 StubReservationRepo

**Files:**
- Modify: `Reservation/src/test/java/org/cleancoders/reservation/usecase/StubReservationRepo.java`

- [ ] **Step 1: 更新方法签名**

```java
@Override
public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
        String roomId, int seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses)
{
    return reservations.values().stream()
            .filter(r -> r.roomId().equals(roomId))
            .filter(r -> r.seatId() == seatId)
            .filter(r -> r.date().equals(date))
            .filter(r -> r.timeSlotId().equals(timeSlotId))
            .filter(r -> statuses.contains(r.status()))
            .findFirst();
}

@Override
public List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> statuses) {
    return reservations.values().stream()
            .filter(r -> r.roomId().equals(roomId))
            .filter(r -> r.seatId() == seatId)
            .filter(r -> statuses.contains(r.status()))
            .toList();
}
```

- [ ] **Step 2: Commit**

```bash
git add Reservation/src/test/java/org/cleancoders/reservation/usecase/StubReservationRepo.java
git commit -m "refactor: StubReservationRepo 适配新接口"
```

---

### Task 33: 编译验证并修复 SystemTask 模块

**Files:**
- Modify: `SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetSeatUsageStatsUseCase.java`
- Modify: `SystemTask/src/main/java/org/cleancoders/systemtask/usecase/GetPopularRoomsStatsUseCase.java`

- [ ] **Step 1: 运行全局编译找错误**

```bash
mvn compile -q 2>&1 | head -50
```

- [ ] **Step 2: 修复 SystemTask 中 seatId 引用**

`GetSeatUsageStatsUseCase` 中 `Reservation::seatId` 现在返回 int，需要适配。
`GetPopularRoomsStatsUseCase` 中 `r.seatId()` 现在返回 int，`seatToRoom` 的 key 类型需要改为 String（用 roomId）。

- [ ] **Step 3: 编译通过后提交**

```bash
git add SystemTask/
git commit -m "fix: SystemTask 模块适配新 Reservation 模型"
```

---

### Task 34: 更新所有测试文件

**Files affected (需要修改的测试):**
- `SeatAndRoom/src/test/java/.../usecase/ManageSeatsUseCaseTest.java` — **删除** (用例已删除)
- `SeatAndRoom/src/test/java/.../usecase/DeleteSeatUseCaseTest.java` — **删除** (用例已删除)
- `SeatAndRoom/src/test/java/.../usecase/ManageRoomsUseCaseTest.java` — **修改**: 适配 Request 新参数
- `SeatAndRoom/src/test/java/.../usecase/UpdateRoomUseCaseTest.java` — **修改**: 适配 Request 新参数
- `SeatAndRoom/src/test/java/.../usecase/UpdateSeatUseCaseTest.java` — **修改**: 适配 (roomId, seatId)
- `SeatAndRoom/src/test/java/.../usecase/ListSeatsUseCaseTest.java` — **修改**: 适配新 Seat(id, roomId, status)
- `SeatAndRoom/src/test/java/.../usecase/DeleteRoomUseCaseTest.java` — **修改**: 适配 StudyRoom 构造
- `Reservation/src/test/java/.../usecase/ReserveUseCaseTest.java` — **修改**: 适配 Request (roomId, seatId)
- `Reservation/src/test/java/.../usecase/CheckInUseCaseTest.java` — **修改**: 适配 Reservation 构造
- `Reservation/src/test/java/.../usecase/CheckOutUseCaseTest.java` — **修改**: 适配 Reservation 构造
- `Reservation/src/test/java/.../usecase/CancelReservationUseCaseTest.java` — **修改**: 适配 Reservation 构造
- `Reservation/src/test/java/.../usecase/ListMyReservationsUseCaseTest.java` — **修改**: 适配新 ReservationItem
- `Reservation/src/test/java/.../usecase/ManageReservationsUseCaseTest.java` — **修改**: 适配新 ReservationItem
- `WebApi/src/test/java/.../AdminResourceTest.java` — **修改**: 移除座位增删测试
- `WebApi/src/test/java/.../AdminResourceIntegrationTest.java` — **修改**: 适配新 API
- `WebApi/src/test/java/.../ReservationResourceTest.java` — **修改**: 适配新 ReserveInput
- `WebApi/src/test/java/.../ReservationResourceIntegrationTest.java` — **修改**: 适配新 ReserveInput
- `WebApi/src/test/java/.../RoomResourceTest.java` — **修改**: 适配新 RoomResponse
- `WebApi/src/test/java/.../RoomResourceIntegrationTest.java` — **修改**: 适配新 API
- `Infrastructure/src/test/java/.../InMemoryRoomRepoTest.java` — **修改**: 适配 StudyRoom 构造
- `Infrastructure/src/test/java/.../ReservationBasedActiveReservationCheckerTest.java` — **修改**: 适配新接口
- `SystemTask/src/test/java/.../` — **修改**: 适配新 Reservation 模型

- [ ] **Step 1: 删除已废弃的测试文件**

```bash
rm SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/ManageSeatsUseCaseTest.java
rm SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCaseTest.java
```

- [ ] **Step 2: 逐个修改保留的测试文件**

对于每个测试文件，将 `Seat` 构造从 `new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE)` 改为 `new Seat(1, "room-1", SeatStatus.AVAILABLE)`。

将 `StudyRoom` 构造从 `new StudyRoom("room-1", "...", "...", 30, RoomStatus.OPEN)` 改为 `new StudyRoom("room-1", "...", "...", RoomLayout.SMALL, RoomStatus.OPEN)`。

将 `Reservation` 构造从 `new Reservation(null, "user-1", "seat-1", "ts-1", date)` 改为 `new Reservation(null, "user-1", "room-1", 1, "ts-1", date)`。

- [ ] **Step 3: 运行全部测试确认通过**

```bash
mvn test
```

修复所有失败测试直至全部通过。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: 更新所有测试适配新座位/教室模型"
```

---

### Task 35: 最终验证和清理

- [ ] **Step 1: 运行完整构建**

```bash
mvn clean compile test
```

- [ ] **Step 2: 修复任何遗留编译错误或测试失败**

- [ ] **Step 3: 检查是否有遗漏的旧 import 或引用**

```bash
git grep "seatNumber" -- ":!*.md" ":!docs/"
git grep "ManageSeatsUseCase" -- ":!*.md" ":!docs/" ":!*.xml"
git grep "DeleteSeatUseCase" -- ":!*.md" ":!docs/" ":!*.xml"
git grep "CreateSeatRequest" -- ":!*.md" ":!docs/"
```

所有结果应仅在 markdown/doc 文件中或为空。

- [ ] **Step 4: 最终 Commit**

```bash
git add -A
git commit -m "chore: 最终清理，确认无遗留引用"
```
