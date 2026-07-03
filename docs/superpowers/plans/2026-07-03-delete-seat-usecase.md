# UC-07 DELETE /api/admin/seats/{id} Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement soft-delete for seats — admin can delete a seat (mark REMOVED), rejected if reserved/occupied or has active reservations. Aligned with DeleteRoomUseCase pattern.

**Architecture:** New `DeleteSeatUseCase` (SeatAndRoom, inherits AdminAuthUseCase). `REMOVED` added to `SeatStatus` + `markRemoved()` on `Seat`. `ActiveReservationChecker` narrow port in Common_Reservation_SeatAndRoom, implemented by `ReservationBasedActiveReservationChecker` in Infrastructure over a new `ReservationRepository.findBySeatIdAndStatusIn` method. Presenter branches on WebApiRoomPresenter, endpoint on AdminResource.

**Tech Stack:** Java 21+, JAX-RS (Jersey), HK2 DI, JUnit 5

## Global Constraints

- AdminAuthUseCase.Presenter / AuthUseCase.Presenter 基类接口需在所有测试桩同时实现
- `WebApiRoomPresenter` 继承 `WebApiPresenter`(持有 `ResponseContext`),注入 `responseContext`
- `SeatAndRoomBinder` 绑定 UseCase/Presenter/Outbound 实现
- `SeatStatus.REMOVED` 在枚举末尾加,不影响现有枚举顺序
- 集成测试需 seed ReservationRepository(TestDataReservationRepo 已预置测试数据,见 `InMemoryReservationRepo`→`TestDataReservationRepo` 的派生关系)

---

### Task 1: Domain — 新增 REMOVED 状态 + Seat.markRemoved()

**Files:**
- Modify: `Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/domain/SeatStatus.java`
- Modify: `Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/domain/Seat.java`

**Interfaces:**
- Produces: `SeatStatus.REMOVED` 枚举值, `Seat.markRemoved()` (public void, throws IllegalStateException if current != AVAILABLE && current != MAINTENANCE)

- [ ] **Step 1: 修改 SeatStatus 枚举**

在 `Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/domain/SeatStatus.java` 末尾加 `REMOVED`:

```java
public enum SeatStatus {
    AVAILABLE,
    RESERVED,
    OCCUPIED,
    MAINTENANCE,
    REMOVED
}
```

- [ ] **Step 2: Seat 新增 markRemoved() 域方法**

在 `Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/domain/Seat.java` 的 `markAvailable()` 方法之后添加:

```java
/**
 * Soft-deletes the seat. Only AVAILABLE or MAINTENANCE seats can be removed.
 *
 * @throws IllegalStateException if the seat is not {@link SeatStatus#AVAILABLE}
 *                               or {@link SeatStatus#MAINTENANCE}
 */
public void markRemoved() {
    if (status != SeatStatus.AVAILABLE && status != SeatStatus.MAINTENANCE) {
        throw new IllegalStateException(
                "只有可用或维护中的座位才能删除，当前状态: " + status);
    }
    this.status = SeatStatus.REMOVED;
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn -pl Common_Reservation_SeatAndRoom -am compile
```

Expected: BUILD SUCCESS (SeatStatus + Seat 编译通过,无其他模块受影响)

- [ ] **Step 4: 提交**

```bash
git add Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/domain/SeatStatus.java Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/domain/Seat.java
git commit -m "feat(Domain): SeatStatus 新增 REMOVED + Seat.markRemoved() 域方法"
```

---

### Task 2: Outbound — 新建 ActiveReservationChecker 窄口 + 扩张 ReservationRepository

**Files:**
- Create: `Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/outbound/ActiveReservationChecker.java`
- Modify: `Reservation/src/main/java/org/cleancoders/reservation/outbound/ReservationRepository.java`
- Modify: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryReservationRepo.java`
- Modify: `Reservation/src/test/java/org/cleancoders/reservation/usecase/StubReservationRepo.java`
- Create: `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationChecker.java`

**Interfaces:**
- Produces: `ActiveReservationChecker.hasActiveForSeat(String seatId): boolean`
- Produces: `ReservationRepository.findBySeatIdAndStatusIn(String seatId, Set<ReservationStatus> statuses): List<Reservation>`
- Produces: `ReservationBasedActiveReservationChecker` (@Singleton, @Inject ReservationRepository, implements ActiveReservationChecker)

- [ ] **Step 1: 创建 ActiveReservationChecker 接口**

新建 `Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/outbound/ActiveReservationChecker.java`:

```java
package org.cleancoders.common_reservation_seatAndRoom.outbound;

/**
 * Narrow port for checking active reservations on a seat before deletion.
 * <p>
 * The definition of "active" (RESERVED / CHECKED_IN) is encapsulated in the
 * infrastructure implementation and is not visible to consumers in the
 * SeatAndRoom module.
 */
public interface ActiveReservationChecker {

    /**
     * Returns {@code true} if the seat has at least one active reservation
     * (RESERVED or CHECKED_IN), meaning it cannot be safely deleted.
     */
    boolean hasActiveForSeat(String seatId);
}
```

- [ ] **Step 2: ReservationRepository 新增 findBySeatIdAndStatusIn 方法**

在 `Reservation/src/main/java/org/cleancoders/reservation/outbound/ReservationRepository.java` 接口末尾添加:

```java
/**
 * Finds all reservations for a seat with any of the given statuses.
 * <p>
 * Unlike the date/time-slot-specific variant, this searches across all dates
 * and time slots — used for cross-cutting checks such as "does this seat have
 * any active reservation?" before deletion.
 */
List<Reservation> findBySeatIdAndStatusIn(String seatId, Set<ReservationStatus> statuses);
```

在文件顶部新增 import:

```java
import java.util.Set;
```

- [ ] **Step 3: InMemoryReservationRepo 实现新方法**

在 `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryReservationRepo.java` 的 `findAll()` 方法之前添加:

```java
@Override
public List<Reservation> findBySeatIdAndStatusIn(String seatId, Set<ReservationStatus> statuses) {
    return store.values().stream()
            .filter(r -> r.seatId().equals(seatId))
            .filter(r -> statuses.contains(r.status()))
            .toList();
}
```

- [ ] **Step 4: StubReservationRepo 实现新方法**

在 `Reservation/src/test/java/org/cleancoders/reservation/usecase/StubReservationRepo.java` 的 `findAll()` 方法之前添加:

```java
@Override
public List<Reservation> findBySeatIdAndStatusIn(String seatId, Set<ReservationStatus> statuses) {
    return reservations.values().stream()
            .filter(r -> r.seatId().equals(seatId))
            .filter(r -> statuses.contains(r.status()))
            .toList();
}
```

- [ ] **Step 5: 创建 ReservationBasedActiveReservationChecker 实现**

新建 `Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationChecker.java`:

```java
package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.common_reservation_seatAndRoom.outbound.ActiveReservationChecker;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.util.Set;

@Singleton
public class ReservationBasedActiveReservationChecker implements ActiveReservationChecker {

    @Inject
    ReservationRepository reservationRepo;

    @Override
    public boolean hasActiveForSeat(String seatId) {
        return !reservationRepo.findBySeatIdAndStatusIn(seatId,
                Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN)).isEmpty();
    }
}
```

- [ ] **Step 6: 编写 ReservationBasedActiveReservationChecker 单元测试**

新建 `Infrastructure/src/test/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationCheckerTest.java`:

```java
package org.cleancoders.infrastructure.persistence;

import org.cleancoders.common_reservation_seatAndRoom.outbound.ActiveReservationChecker;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.usecase.StubReservationRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationBasedActiveReservationCheckerTest
{
    private StubReservationRepo repo;
    private ActiveReservationChecker checker;

    @BeforeEach
    void setUp()
    {
        repo = new StubReservationRepo();
        checker = new ReservationBasedActiveReservationChecker();
        checker.reservationRepo = repo;
    }

    @Test
    void shouldReturnTrueWhenReservedExists()
    {
        repo.save(new Reservation("r1", "user-1", "seat-1", "ts-1", LocalDate.now()));
        assertTrue(checker.hasActiveForSeat("seat-1"));
    }

    @Test
    void shouldReturnTrueWhenCheckedInExists()
    {
        Reservation r = new Reservation("r2", "user-1", "seat-1", "ts-1", LocalDate.now());
        r.checkIn();
        repo.save(r);
        assertTrue(checker.hasActiveForSeat("seat-1"));
    }

    @Test
    void shouldReturnFalseWhenAllCancelledOrExpired()
    {
        Reservation cancelled = new Reservation("r3", "user-1", "seat-1", "ts-1", LocalDate.now());
        cancelled.cancel();
        repo.save(cancelled);

        Reservation expired = new Reservation("r4", "user-2", "seat-1", "ts-2", LocalDate.now());
        expired.expire();
        repo.save(expired);

        assertFalse(checker.hasActiveForSeat("seat-1"));
    }

    @Test
    void shouldReturnFalseWhenNoReservations()
    {
        assertFalse(checker.hasActiveForSeat("seat-1"));
    }
}
```

- [ ] **Step 7: 编译验证**

```bash
mvn -pl Common_Reservation_SeatAndRoom,Reservation,Infrastructure -am compile
```

Expected: BUILD SUCCESS

- [ ] **Step 8: 运行 Infrastructure 单元测试**

```bash
mvn -pl Infrastructure -am test -Dtest=ReservationBasedActiveReservationCheckerTest
```

Expected: Tests run: 4, Failures: 0

- [ ] **Step 9: 提交**

```bash
git add Common_Reservation_SeatAndRoom/src/main/java/org/cleancoders/common_reservation_seatAndRoom/outbound/ActiveReservationChecker.java Reservation/src/main/java/org/cleancoders/reservation/outbound/ReservationRepository.java Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/InMemoryReservationRepo.java Reservation/src/test/java/org/cleancoders/reservation/usecase/StubReservationRepo.java Infrastructure/src/main/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationChecker.java Infrastructure/src/test/java/org/cleancoders/infrastructure/persistence/ReservationBasedActiveReservationCheckerTest.java
git commit -m "feat(Outbound): 新增 ActiveReservationChecker 窄口 + ReservationRepository.findBySeatIdAndStatusIn"
```

---

### Task 3: UseCase — 新建 DeleteSeatUseCase

**Files:**
- Create: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCase.java`

**Interfaces:**
- Consumes: `SeatRepository.findById`, `SeatRepository.save`, `ActiveReservationChecker.hasActiveForSeat(String)`, `Seat.markRemoved()`, `SeatStatus.REMOVED`
- Produces: `DeleteSeatUseCase` inheriting `AdminAuthUseCase<DeleteSeatUseCase.Request, DeleteSeatUseCase.Output>`
- Presenter 方法: `deleteSuccess(String)`, `seatNotFound(String)`, `seatAlreadyRemoved(String)`, `seatInUse(String, SeatStatus)`, `seatHasActiveReservations(String)`

- [ ] **Step 1: 创建 DeleteSeatUseCase + 单元测试**

新建 `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCase.java`:

```java
package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.ActiveReservationChecker;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;

/**
 * UC-07: 删除座位(管理员,软删除)。
 * <p>
 * 将可用或维护中的座位标记为 REMOVED。RESERVED/OCCUPIED 的座位或有
 * 活跃预约(RESERVED/CHECKED_IN)的座位拒绝删除,返回 409。
 */
public class DeleteSeatUseCase extends AdminAuthUseCase<DeleteSeatUseCase.Request, DeleteSeatUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    SeatRepository seatRepo;

    @Inject
    ActiveReservationChecker activeReservationChecker;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = seatRepo.findById(req.seatId());
        if (existing.isEmpty())
        {
            presenter.seatNotFound(req.seatId());
            return null;
        }

        Seat seat = existing.get();
        if (seat.status() == SeatStatus.REMOVED)
        {
            presenter.seatAlreadyRemoved(req.seatId());
            return null;
        }

        // 域级保护: RESERVED/OCCUPIED 不得删除
        if (seat.status() != SeatStatus.AVAILABLE
                && seat.status() != SeatStatus.MAINTENANCE)
        {
            presenter.seatInUse(req.seatId(), seat.status());
            return null;
        }

        // 活跃预约检查: 查预约表, 有 RESERVED/CHECKED_IN 则拒绝
        if (activeReservationChecker.hasActiveForSeat(req.seatId()))
        {
            presenter.seatHasActiveReservations(req.seatId());
            return null;
        }

        seat.markRemoved();
        Seat saved = seatRepo.save(seat);
        presenter.deleteSuccess(saved.id());
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void deleteSuccess(String seatId);

        void seatNotFound(String seatId);

        void seatAlreadyRemoved(String seatId);

        void seatInUse(String seatId, SeatStatus current);

        void seatHasActiveReservations(String seatId);
    }

    public record Request(String token, String seatId)
            implements AuthUseCase.Request
    {
    }

    public record Output(String seatId)
    {
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl SeatAndRoom -am compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCase.java
git commit -m "feat(SeatAndRoom): 新增 DeleteSeatUseCase (UC-07 软删除座位)"
```

---

### Task 4: 单元测试 — DeleteSeatUseCaseTest

**Files:**
- Create: `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCaseTest.java`

**Interfaces:**
- Consumes: `StubTokenService`, `StubUserRepo`(Common_Test_Infrastructure), `StubSeatRepo`(Common_Reservation_SeatAndRoom_Test_Infrastructure), `AdminAuthUseCase.Presenter.forbidden()`, `AuthUseCase.Presenter.invalidToken()`/`userNotFound()`
- 内嵌 `StubActiveReservationChecker` (匿名实现或独立 stub)

- [ ] **Step 1: 新建 DeleteSeatUseCaseTest**

新建 `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCaseTest.java`:

```java
package org.cleancoders.seatandroom.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.ActiveReservationChecker;
import org.cleancoders.common_reservation_seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.common_test_infrastructure.StubTokenService;
import org.cleancoders.common_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DeleteSeatUseCaseTest
{

    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private DeleteSeatUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubSeatRepo seatRepo;
    private StubActiveReservationChecker activeReservationChecker;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(ADMIN_ID);
        userRepo = new StubUserRepo();
        seatRepo = new StubSeatRepo();
        activeReservationChecker = new StubActiveReservationChecker();
        presenter = new StubPresenter();

        useCase = new DeleteSeatUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.seatRepo = seatRepo;
        useCase.activeReservationChecker = activeReservationChecker;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
    }

    @Test
    void shouldRemoveAvailableSeat()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-1"));

        assertNotNull(output);
        assertEquals("seat-1", output.seatId());
        assertTrue(presenter.deleteSuccessCalled);
        assertEquals("seat-1", presenter.deleteSuccessSeatId.get());
        assertEquals(SeatStatus.REMOVED, seatRepo.findById("seat-1").get().status());
    }

    @Test
    void shouldRemoveMaintenanceSeat()
    {
        seatRepo.addSeat(new Seat("seat-m", "room-1", "M-1", SeatStatus.MAINTENANCE));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-m"));

        assertNotNull(output);
        assertEquals("seat-m", output.seatId());
        assertTrue(presenter.deleteSuccessCalled);
        assertEquals(SeatStatus.REMOVED, seatRepo.findById("seat-m").get().status());
    }

    @Test
    void shouldReturnSeatNotFound()
    {
        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "nonexistent"));

        assertNull(output);
        assertTrue(presenter.seatNotFoundCalled);
        assertEquals("nonexistent", presenter.seatNotFoundId.get());
    }

    @Test
    void shouldReturnSeatAlreadyRemoved()
    {
        seatRepo.addSeat(new Seat("seat-r", "room-1", "R-1", SeatStatus.REMOVED));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-r"));

        assertNull(output);
        assertTrue(presenter.seatAlreadyRemovedCalled);
        assertEquals("seat-r", presenter.seatAlreadyRemovedId.get());
    }

    @Test
    void shouldRejectReservedSeat()
    {
        seatRepo.addSeat(new Seat("seat-r", "room-1", "R-1", SeatStatus.RESERVED));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-r"));

        assertNull(output);
        assertTrue(presenter.seatInUseCalled);
        assertEquals("seat-r", presenter.seatInUseSeatId.get());
        assertEquals(SeatStatus.RESERVED, presenter.seatInUseCurrent.get());
    }

    @Test
    void shouldRejectOccupiedSeat()
    {
        seatRepo.addSeat(new Seat("seat-o", "room-1", "O-1", SeatStatus.OCCUPIED));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-o"));

        assertNull(output);
        assertTrue(presenter.seatInUseCalled);
        assertEquals(SeatStatus.OCCUPIED, presenter.seatInUseCurrent.get());
    }

    @Test
    void shouldRejectWhenActiveReservationsExist()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));
        activeReservationChecker.hasActiveForSeatResult = true;

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-1"));

        assertNull(output);
        assertTrue(presenter.seatHasActiveReservationsCalled);
        assertEquals("seat-1", presenter.seatHasActiveReservationsId.get());
        // Seat status must remain unchanged
        assertEquals(SeatStatus.AVAILABLE, seatRepo.findById("seat-1").get().status());
    }

    @Test
    void shouldAllowWhenOnlyHistoricalReservations()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));
        activeReservationChecker.hasActiveForSeatResult = false;

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-1"));

        assertNotNull(output);
        assertTrue(presenter.deleteSuccessCalled);
    }

    @Test
    void shouldRejectNonAdminUser()
    {
        tokenService.setUserId(STUDENT_ID);
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new DeleteSeatUseCase.Request(STUDENT_TOKEN, "seat-1"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubPresenter implements
            DeleteSeatUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean deleteSuccessCalled = false;
        AtomicReference<String> deleteSuccessSeatId = new AtomicReference<>();
        boolean seatNotFoundCalled = false;
        AtomicReference<String> seatNotFoundId = new AtomicReference<>();
        boolean seatAlreadyRemovedCalled = false;
        AtomicReference<String> seatAlreadyRemovedId = new AtomicReference<>();
        boolean seatInUseCalled = false;
        AtomicReference<String> seatInUseSeatId = new AtomicReference<>();
        AtomicReference<SeatStatus> seatInUseCurrent = new AtomicReference<>();
        boolean seatHasActiveReservationsCalled = false;
        AtomicReference<String> seatHasActiveReservationsId = new AtomicReference<>();
        boolean forbiddenCalled = false;

        @Override
        public void deleteSuccess(String seatId)
        {
            deleteSuccessCalled = true;
            deleteSuccessSeatId.set(seatId);
        }

        @Override
        public void seatNotFound(String seatId)
        {
            seatNotFoundCalled = true;
            seatNotFoundId.set(seatId);
        }

        @Override
        public void seatAlreadyRemoved(String seatId)
        {
            seatAlreadyRemovedCalled = true;
            seatAlreadyRemovedId.set(seatId);
        }

        @Override
        public void seatInUse(String seatId, SeatStatus current)
        {
            seatInUseCalled = true;
            seatInUseSeatId.set(seatId);
            seatInUseCurrent.set(current);
        }

        @Override
        public void seatHasActiveReservations(String seatId)
        {
            seatHasActiveReservationsCalled = true;
            seatHasActiveReservationsId.set(seatId);
        }

        @Override
        public void forbidden()
        {
            forbiddenCalled = true;
        }

        @Override
        public void invalidToken()
        {
            fail("invalidToken() must not be called");
        }

        @Override
        public void userNotFound()
        {
            fail("userNotFound() must not be called");
        }
    }

    static class StubActiveReservationChecker implements ActiveReservationChecker
    {
        boolean hasActiveForSeatResult = false;

        @Override
        public boolean hasActiveForSeat(String seatId)
        {
            return hasActiveForSeatResult;
        }
    }
}
```

- [ ] **Step 2: 运行测试并确认全部通过**

```bash
mvn -pl SeatAndRoom -am test -Dtest=DeleteSeatUseCaseTest
```

Expected: Tests run: 9, Failures: 0

- [ ] **Step 3: 提交**

```bash
git add SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/DeleteSeatUseCaseTest.java
git commit -m "test(SeatAndRoom): 新增 DeleteSeatUseCaseTest (9 用例)"
```

---

### Task 5: Presenter — WebApiRoomPresenter 实现 DeleteSeatUseCase.Presenter

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java`

**Interfaces:**
- Consumes: `DeleteSeatUseCase.Presenter` 接口, `SeatStatus.name()`, `ResponseContext.set(Response)`

- [ ] **Step 1: 修改 WebApiRoomPresenter**

在 `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java` 的 implements 列表中,`UpdateSeatUseCase.Presenter` 后添加 `DeleteSeatUseCase.Presenter`:

将:
```java
        UpdateSeatUseCase.Presenter
{
```
改为:
```java
        UpdateSeatUseCase.Presenter,
        DeleteSeatUseCase.Presenter
{
```

然后在 `invalidStatus` 方法之后加入 `DeleteSeatUseCase.Presenter` 的 5 个实现方法:

```java
    // --- DeleteSeatUseCase (UC-07 delete seat) ---

    @Override
    public void deleteSuccess(String seatId)
    {
        responseContext.set(Response.status(200).entity(Map.of(
                "message", "座位已删除",
                "seatId", seatId
        )).build());
    }

    @Override
    public void seatAlreadyRemoved(String seatId)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "座位已处于删除状态",
                "seatId", seatId
        )).build());
    }

    @Override
    public void seatInUse(String seatId, SeatStatus current)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "座位正在使用中，无法删除",
                "seatId", seatId,
                "currentStatus", current.name()
        )).build());
    }

    @Override
    public void seatHasActiveReservations(String seatId)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "座位存在活跃预约，无法删除",
                "seatId", seatId
        )).build());
    }
```

- [ ] **Step 2: 编译验证**

```bash
mvn -pl WebApi -am compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java
git commit -m "feat(WebApi): WebApiRoomPresenter 实现 DeleteSeatUseCase.Presenter"
```

---

### Task 6: Resource + Binder — AdminResource 端点 + SeatAndRoomBinder 绑定

**Files:**
- Modify: `WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java`
- Modify: `WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java`

**Interfaces:**
- Consumes: `DeleteSeatUseCase`, `DeleteSeatUseCase.Presenter`, `ActiveReservationChecker`
- 新增端点: `DELETE /admin/seats/{id}` (注解 @DELETE @Path("/seats/{id}"), @CookieParam "Authorization", @PathParam "id")

- [ ] **Step 1: AdminResource 新增端点**

首先在 `AdminResource.java` 顶部 import 区添加:

```java
import org.cleancoders.seatandroom.usecase.DeleteSeatUseCase;
```

然后在 `@Inject` 块中(紧跟 `@Inject UpdateSeatUseCase updateSeatUseCase;` 之后)添加:

```java
    @Inject
    DeleteSeatUseCase deleteSeatUseCase;
```

最后在 `deleteRoom` 端点之后、类结束 `}` 之前添加 `deleteSeat` 端点:

```java
    @DELETE
    @Path("/seats/{id}")
    @Operation(summary = "删除座位 (UC-07)",
            description = "管理员软删除座位(转 REMOVED)。座位不存在返回 404,已删除/处于 RESERVED-OCCUPIED/有活跃预约返回 409。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "座位不存在"),
            @ApiResponse(responseCode = "409", description = "座位已删除 / 正在使用中 / 存在活跃预约")
    })
    public Response deleteSeat(
            @CookieParam("Authorization") String authCookie,
            @Parameter(description = "座位ID", required = true, example = "seat-1")
            @PathParam("id") String seatId)
    {
        deleteSeatUseCase.execute(new DeleteSeatUseCase.Request(authCookie, seatId));
        return responseContext.get();
    }
```

- [ ] **Step 2: SeatAndRoomBinder 绑定**

在 `WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java` 中添加 import:

```java
import org.cleancoders.common_reservation_seatAndRoom.outbound.ActiveReservationChecker;
import org.cleancoders.infrastructure.persistence.ReservationBasedActiveReservationChecker;
```

在 `configure()` 方法的 UseCase 绑定区追加:

```java
        bind(DeleteSeatUseCase.class).to(DeleteSeatUseCase.class);
```

在 Presenter 绑定区(WebApiRoomPresenter 的 `.to(...)` 链尾)追加:

```java
                .to(DeleteSeatUseCase.Presenter.class)
```

在 Infrastructure 绑定区追加:

```java
        bind(ReservationBasedActiveReservationChecker.class).to(ActiveReservationChecker.class).in(Singleton.class);
```

最终 Binder 的 configure() 方法变为:

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
        bind(ManageSeatsUseCase.class).to(ManageSeatsUseCase.class);
        bind(UpdateSeatUseCase.class).to(UpdateSeatUseCase.class);
        bind(DeleteSeatUseCase.class).to(DeleteSeatUseCase.class);

        // === Presenters ===
        bind(WebApiRoomPresenter.class)
                .to(ListRoomsUseCase.Presenter.class)
                .to(ListSeatsUseCase.Presenter.class)
                .to(ManageRoomsUseCase.Presenter.class)
                .to(UpdateRoomUseCase.Presenter.class)
                .to(DeleteRoomUseCase.Presenter.class)
                .to(ManageSeatsUseCase.Presenter.class)
                .to(UpdateSeatUseCase.Presenter.class)
                .to(DeleteSeatUseCase.Presenter.class)
                .in(Singleton.class);

        // === Infrastructure ===
        bind(TestDataSeatRepo.class).to(SeatRepository.class).in(Singleton.class);
        bind(TestDataTimeSlotRepo.class).to(TimeSlotRepository.class).in(Singleton.class);
        bind(TestDataRoomRepo.class).to(RoomRepository.class).in(Singleton.class);
        bind(ReservationBasedActiveReservationChecker.class).to(ActiveReservationChecker.class).in(Singleton.class);
    }
```

- [ ] **Step 3: 编译验证**

```bash
mvn -pl WebApi -am compile
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java
git commit -m "feat(WebApi): AdminResource 新增 DELETE /api/admin/seats/{id} (UC-07 删除座位)"
```

---

### Task 7: 集成测试 — AdminResourceIntegrationTest 新增 delete seat 测试

**Files:**
- Modify: `WebApi/src/test/java/org/cleancoders/web/resource/AdminResourceIntegrationTest.java`

**Interfaces:**
- Consumes: 预置座位数据(`TestDataSeatRepo` 派生自 `InMemorySeatRepo`), 预置预约数据(`TestDataReservationRepo` 派生自 `InMemoryReservationRepo`)
- 关键数据映射:
  - `seat-1`: AVAILABLE(基类 seed) + RESERVED res-1(TestDataReservationRepo) → 删除 409(有活跃预约)
  - `seat-2`: AVAILABLE(基类 seed) + CANCELLED res-6 → 删除 200(仅历史预约)
  - `seat-3`: AVAILABLE(基类 seed) + RESERVED res-2 → 删除 409
  - `seat-13`: AVAILABLE(TestDataSeatRepo 追加) + 无预约 → 删除 200
  - `seat-15`: RESERVED(TestDataSeatRepo) → 删除 409(seatInUse)
  - `seat-16`: MAINTENANCE(TestDataSeatRepo) → 删除 200
  - `seat-17`: OCCUPIED(TestDataSeatRepo) → 删除 409(seatInUse)

- [ ] **Step 1: 添加集成测试用例**

`WebApi/src/test/java/org/cleancoders/web/resource/AdminResourceIntegrationTest.java` 需新增 import:

```java
import jakarta.ws.rs.core.GenericType;
import java.util.Map;
```

在文件末尾 `shouldReturn403WhenStudentDeletesRoom` 测试之后、类结束 `}` 之前添加 delete seat 测试:

```java
    // --- delete seat tests ---

    @Test
    void shouldReturn200WhenAdminDeletesAvailableSeat()
    {
        // seat-13: AVAILABLE (TestDataSeatRepo), no reservations targeting it
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-13")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位已删除", body.get("message"));
        assertEquals("seat-13", body.get("seatId"));

        // Verify seat is now REMOVED
        Seat seat = seatRepo.findById("seat-13").get();
        assertEquals(SeatStatus.REMOVED, seat.status());
    }

    @Test
    void shouldReturn200WhenAdminDeletesMaintenanceSeat()
    {
        // seat-16: MAINTENANCE (TestDataSeatRepo)
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-16")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        assertEquals(SeatStatus.REMOVED, seatRepo.findById("seat-16").get().status());
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentSeat()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位不存在", body.get("error"));
        assertEquals("nonexistent", body.get("seatId"));
    }

    @Test
    void shouldReturn409WhenSeatAlreadyRemoved()
    {
        seatRepo.save(new Seat("seat-removed", "room-1", "Z-1", SeatStatus.REMOVED));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-removed")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位已处于删除状态", body.get("error"));
        assertEquals("seat-removed", body.get("seatId"));
    }

    @Test
    void shouldReturn409WhenSeatReserved()
    {
        // seat-15: RESERVED (TestDataSeatRepo)
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-15")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位正在使用中，无法删除", body.get("error"));
        assertEquals("seat-15", body.get("seatId"));
        assertEquals("RESERVED", body.get("currentStatus"));
    }

    @Test
    void shouldReturn409WhenSeatHasActiveReservations()
    {
        // seat-1: AVAILABLE 但 TestDataReservationRepo 有 RESERVED res-1
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位存在活跃预约，无法删除", body.get("error"));
        assertEquals("seat-1", body.get("seatId"));
    }

    @Test
    void shouldReturn200WhenSeatOnlyHasCancelledReservations()
    {
        // seat-2: AVAILABLE, res-6 is CANCELLED (not active)
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-2")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位已删除", body.get("message"));
        assertEquals("seat-2", body.get("seatId"));
    }

    @Test
    void shouldReturn403WhenStudentDeletesSeat()
    {
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/seats/seat-13")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .delete();

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn401WhenNoTokenForSeatDelete()
    {
        Response response = target("/admin/seats/seat-13")
                .request(MediaType.APPLICATION_JSON)
                .delete();

        assertEquals(401, response.getStatus());
    }
```

- [ ] **Step 2: 运行集成测试**

```bash
mvn -pl WebApi -am test -Dtest=AdminResourceIntegrationTest
```

Expected: All delete-seat tests pass (9 new), no regressions on existing tests

- [ ] **Step 3: 提交**

```bash
git add WebApi/src/test/java/org/cleancoders/web/resource/AdminResourceIntegrationTest.java
git commit -m "test(WebApi): AdminResource 新增 DELETE seat 集成测试 (9 用例)"
```

---

### Task 8: 全量回归验证

- [ ] **Step 1: 运行 SeatAndRoom + WebApi + Infrastructure + Common_Reservation_SeatAndRoom 全量测试**

```bash
mvn -pl SeatAndRoom,WebApi,Infrastructure,Common_Reservation_SeatAndRoom -am test
```

Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 2: 运行 Reservation 模块测试确认无回归**

```bash
mvn -pl Reservation -am test
```

Expected: BUILD SUCCESS (StubReservationRepo 新方法不影响既有测试)

- [ ] **Step 3: 最终提交(如有未提交的变更)**

```bash
git status
```

确认所有变更已提交。若有遗漏的 import 或格式调整,提交之。
