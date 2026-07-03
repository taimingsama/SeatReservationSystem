# UC-07 PUT /api/admin/seats/{id} Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 `PUT /api/admin/seats/{id}`(UC-07 更新座位状态),管理员可将座位在 `AVAILABLE` ↔ `MAINTENANCE` 之间切换。

**Architecture:** 新增独立 `UpdateSeatUseCase`(SeatAndRoom 模块,对齐 UC-06 拆分模式),继承 `AdminAuthUseCase`。复用 `Seat` 现有域方法 `markMaintenance()` / `markAvailable()`(不靠异常控流,先检查当前状态再调用)。WebApi 层新增 `UpdateSeatRequest` DTO,在 `WebApiRoomPresenter` 加 presenter 分支,`AdminResource` 加端点,`SeatAndRoomBinder` 补绑定。不修改 `Seat` 领域模型,不扩张 `SeatRepository` 接口。

**Tech Stack:** Java 17+, Jakarta CDI/HK2 注入,Jersey (JAX-RS),JUnit 5,JerseyTest 集成测试,Maven 多模块。

## Global Constraints

- 复用现有域方法 `Seat.markMaintenance()`(AVAILABLE→MAINTENANCE)与 `Seat.markAvailable()`(MAINTENANCE→AVAILABLE),**不修改 `Seat` 类**,不动 `seatNumber`/`roomId`(均为 final)。
- `status` 请求体取值限定 `AVAILABLE` / `MAINTENANCE`;`RESERVED`/`OCCUPIED` 由预约流程流转,不接受通过本接口设置(返回 400);非法字符串与 null 返回 400。
- 不靠异常控流:用 `if` 显式比对 `seat.status()` 再调域方法,不依赖 `markMaintenance` 抛出的 `IllegalStateException`。
- **不扩张 `SeatRepository` 接口**(`findById`+`save` 已够),避免波及 5 个 repo 实现。
- `ManageSeatsUseCase` 保持只管创建;**新增 `UpdateSeatUseCase` 独立类**,不向其加方法。
- Presenter impl 加在 `WebApiRoomPresenter`(沿用 POST seats 的选择);`WebApiAdminPresenter` 不动。
- 错误消息中文,与现有端点一致(`自习室不存在` / `座位编号已存在` 风格)。
- 构建验证:`mvn -pl SeatAndRoom,WebApi -am test` 全量通过。
- 提交信息末尾追加 `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`。

---

## File Structure

| 文件 | 责任 | 动作 |
|---|---|---|
| `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCase.java` | 用例:校验座位存在 → 校验 status → 状态转换 → 保存 → presenter | Create |
| `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCaseTest.java` | 单元测试(6 个用例 + StubPresenter) | Create |
| `WebApi/src/main/java/org/cleancoders/web/dto/admin/UpdateSeatRequest.java` | 请求体 DTO(`status` 单字段) | Create |
| `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java` | 实现 `UpdateSeatUseCase.Presenter` 的 4 个分支 | Modify |
| `WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java` | 绑定 `UpdateSeatUseCase` + 其 Presenter | Modify |
| `WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java` | 新增 `PUT /admin/seats/{id}` 端点 + `@Inject` | Modify |
| `WebApi/src/test/java/org/cleancoders/web/resource/AdminResourceIntegrationTest.java` | 8 个集成测试 | Modify |

复用(不修改):`Seat`、`SeatRepository`、`SeatResponse`、`StubSeatRepo`、`InMemorySeatRepo`、`ResponseContext`、`WebApiPresenter` 基类(认证分支)。

---

## Task 1: UpdateSeatUseCase + 单元测试

**Files:**
- Create: `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCase.java`
- Test: `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCaseTest.java`

**Interfaces:**
- Consumes: `AdminAuthUseCase<R extends AuthUseCase.Request, O>`(基类,提供 `tokenService`/`userRepo`/`presenter` 字段与 `execute(Request)` 入口;`doExecute(User, Request)` 为抽象钩子);`SeatRepository.findById(String)`→`Optional<Seat>`、`SeatRepository.save(Seat)`→`Seat`;`Seat.markMaintenance()` / `Seat.markAvailable()` / `Seat.status()` / `Seat.id()`;`SeatStatus` 枚举(AVAILABLE/RESERVED/OCCUPIED/MAINTENANCE);测试 stub `StubSeatRepo`(`addSeat(Seat)` 种子 + `findById`/`save`)、`StubTokenService`(`setUserId(String)`)、`StubUserRepo`(`addUser(User)`)、`StubRoomRepo`。
- Produces: `UpdateSeatUseCase` 类(`@Inject Presenter presenter`、`@Inject SeatRepository seatRepo`);`UpdateSeatUseCase.Request(String token, String seatId, String status)`;`UpdateSeatUseCase.Output(String seatId)`;`UpdateSeatUseCase.Presenter` 接口含 `updateSuccess(Seat)` / `seatNotFound(String)` / `invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target)` / `invalidStatus(String seatId, String status)`。Task 2 的 presenter impl 与 resource 将消费这些签名。

- [ ] **Step 1: 先建 UseCase 骨架(让测试可编译)**

Create `SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCase.java`:

```java
package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;

/**
 * UC-07: 更新座位状态(管理员)。仅允许 AVAILABLE ↔ MAINTENANCE 切换,
 * 复用 {@link Seat#markMaintenance()} / {@link Seat#markAvailable()} 域方法。
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
        return null;
    }

    public interface Presenter
    {
        void updateSuccess(Seat seat);

        void seatNotFound(String seatId);

        void invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target);

        void invalidStatus(String seatId, String status);
    }

    public record Request(String token, String seatId, String status)
            implements AuthUseCase.Request
    {
    }

    public record Output(String seatId)
    {
    }
}
```

- [ ] **Step 2: 写第一个失败测试(成功切换 AVAILABLE→MAINTENANCE)**

Create `SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCaseTest.java`:

```java
package org.cleancoders.seatandroom.usecase;

import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UpdateSeatUseCaseTest
{
    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private UpdateSeatUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubSeatRepo seatRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(ADMIN_ID);
        userRepo = new StubUserRepo();
        seatRepo = new StubSeatRepo();
        presenter = new StubPresenter();

        useCase = new UpdateSeatUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.seatRepo = seatRepo;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
    }

    @Test
    void shouldMarkMaintenanceFromAvailable()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "MAINTENANCE"));

        assertNotNull(output);
        assertEquals("seat-1", output.seatId());
        assertTrue(presenter.updateSuccessCalled);
        assertEquals(SeatStatus.MAINTENANCE, presenter.updatedSeat.get().status());
    }

    // --- Stubs ---

    static class StubPresenter implements
            UpdateSeatUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean updateSuccessCalled = false;
        AtomicReference<Seat> updatedSeat = new AtomicReference<>();
        boolean seatNotFoundCalled = false;
        AtomicReference<String> seatNotFoundId = new AtomicReference<>();
        boolean invalidStatusTransitionCalled = false;
        AtomicReference<String> transitionSeatId = new AtomicReference<>();
        AtomicReference<SeatStatus> transitionCurrent = new AtomicReference<>();
        AtomicReference<SeatStatus> transitionTarget = new AtomicReference<>();
        boolean invalidStatusCalled = false;
        AtomicReference<String> invalidStatusSeatId = new AtomicReference<>();
        AtomicReference<String> invalidStatusValue = new AtomicReference<>();
        boolean forbiddenCalled = false;

        @Override
        public void updateSuccess(Seat seat)
        {
            updateSuccessCalled = true;
            updatedSeat.set(seat);
        }

        @Override
        public void seatNotFound(String seatId)
        {
            seatNotFoundCalled = true;
            seatNotFoundId.set(seatId);
        }

        @Override
        public void invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target)
        {
            invalidStatusTransitionCalled = true;
            transitionSeatId.set(seatId);
            transitionCurrent.set(current);
            transitionTarget.set(target);
        }

        @Override
        public void invalidStatus(String seatId, String status)
        {
            invalidStatusCalled = true;
            invalidStatusSeatId.set(seatId);
            invalidStatusValue.set(status);
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
}
```

- [ ] **Step 3: 运行测试,确认失败**

Run: `mvn -pl SeatAndRoom -am test -Dtest=UpdateSeatUseCaseTest`
Expected: FAIL — `shouldMarkMaintenanceFromAvailable` 因 `doExecute` 返回 null,`assertNotNull(output)` 失败。

- [ ] **Step 4: 实现 doExecute(成功路径 + seatNotFound + 非法转换 + 非法状态)**

Replace the `doExecute` 方法体(及顶部 imports 已含 `SeatStatus`)为:

```java
    @Override
    private Output doExecute(User user, Request req)
    {
        var existing = seatRepo.findById(req.seatId());
        if (existing.isEmpty())
        {
            presenter.seatNotFound(req.seatId());
            return null;
        }

        Seat seat = existing.get();

        if (req.status() == null)
        {
            presenter.invalidStatus(req.seatId(), null);
            return null;
        }

        SeatStatus target;
        try
        {
            target = SeatStatus.valueOf(req.status());
        }
        catch (IllegalArgumentException e)
        {
            presenter.invalidStatus(req.seatId(), req.status());
            return null;
        }

        // 仅允许管理员可控两态;RESERVED/OCCUPIED 由预约流程流转
        if (target != SeatStatus.AVAILABLE && target != SeatStatus.MAINTENANCE)
        {
            presenter.invalidStatus(req.seatId(), req.status());
            return null;
        }

        // 不靠异常控流:先检查当前状态,只调合法路径
        SeatStatus current = seat.status();
        if (target == SeatStatus.MAINTENANCE)
        {
            if (current != SeatStatus.AVAILABLE)
            {
                presenter.invalidStatusTransition(req.seatId(), current, target);
                return null;
            }
            seat.markMaintenance();
        }
        else // AVAILABLE
        {
            if (current != SeatStatus.MAINTENANCE)
            {
                presenter.invalidStatusTransition(req.seatId(), current, target);
                return null;
            }
            seat.markAvailable();
        }

        Seat saved = seatRepo.save(seat);
        presenter.updateSuccess(saved);
        return new Output(saved.id());
    }
```

- [ ] **Step 5: 运行测试,确认通过**

Run: `mvn -pl SeatAndRoom -am test -Dtest=UpdateSeatUseCaseTest`
Expected: PASS(1/1)。

- [ ] **Step 6: 追加其余 5 个失败测试**

在 `UpdateSeatUseCaseTest` 的 `shouldMarkMaintenanceFromAvailable` 之后、`// --- Stubs ---` 之前插入:

```java
    @Test
    void shouldMarkAvailableFromMaintenance()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.MAINTENANCE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "AVAILABLE"));

        assertNotNull(output);
        assertTrue(presenter.updateSuccessCalled);
        assertEquals(SeatStatus.AVAILABLE, presenter.updatedSeat.get().status());
    }

    @Test
    void shouldReturnSeatNotFound()
    {
        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "nonexistent", "MAINTENANCE"));

        assertNull(output);
        assertTrue(presenter.seatNotFoundCalled);
        assertEquals("nonexistent", presenter.seatNotFoundId.get());
    }

    @Test
    void shouldRejectInvalidStatusTransition()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.RESERVED));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "MAINTENANCE"));

        assertNull(output);
        assertTrue(presenter.invalidStatusTransitionCalled);
        assertEquals("seat-1", presenter.transitionSeatId.get());
        assertEquals(SeatStatus.RESERVED, presenter.transitionCurrent.get());
        assertEquals(SeatStatus.MAINTENANCE, presenter.transitionTarget.get());
    }

    @Test
    void shouldRejectInvalidStatusValue()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "BROKEN"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals("seat-1", presenter.invalidStatusSeatId.get());
        assertEquals("BROKEN", presenter.invalidStatusValue.get());
    }

    @Test
    void shouldRejectReservedAsTargetStatus()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "RESERVED"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
    }

    @Test
    void shouldRejectNonAdminUser()
    {
        tokenService.setUserId(STUDENT_ID);
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                STUDENT_TOKEN, "seat-1", "MAINTENANCE"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }
```

- [ ] **Step 7: 运行全部单元测试,确认通过**

Run: `mvn -pl SeatAndRoom -am test -Dtest=UpdateSeatUseCaseTest`
Expected: PASS(6/6)。

- [ ] **Step 8: 运行 SeatAndRoom 模块全量测试,确认无回归**

Run: `mvn -pl SeatAndRoom -am test`
Expected: 全绿(含既有 `ManageSeatsUseCaseTest` / `UpdateRoomUseCaseTest` 等)。

- [ ] **Step 9: 提交**

```bash
git add SeatAndRoom/src/main/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCase.java \
        SeatAndRoom/src/test/java/org/cleancoders/seatandroom/usecase/UpdateSeatUseCaseTest.java
git commit -m "feat(SeatAndRoom): 新增 UpdateSeatUseCase (UC-07 更新座位状态)

管理员可将座位在 AVAILABLE↔MAINTENANCE 之间切换,复用 Seat 域方法
markMaintenance/markAvailable,先检查当前状态再调用,不靠异常控流。
非法状态值/RESERVED/OCCUPIED 返回 invalidStatus,非法转换返回
invalidStatusTransition,座位不存在返回 seatNotFound。

- 新增 UpdateSeatUseCase (对齐 UC-06 拆分模式,ManageSeatsUseCase 保持只管创建)
- 6 个单元测试,复用现有 StubSeatRepo,无新增测试基础设施
- 复用现有 SeatRepository.findById/save,接口零扩张

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: WebApi 接入(DTO + Presenter + Binder + Resource)与集成测试

**Files:**
- Create: `WebApi/src/main/java/org/cleancoders/web/dto/admin/UpdateSeatRequest.java`
- Modify: `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java`(implements 列表 + 4 个方法)
- Modify: `WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java`(2 行绑定)
- Modify: `WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java`(`@Inject` 字段 + `updateSeat` 端点)
- Test: `WebApi/src/test/java/org/cleancoders/web/resource/AdminResourceIntegrationTest.java`(追加 8 个测试)

**Interfaces:**
- Consumes: Task 1 的 `UpdateSeatUseCase` 及其 `Request`/`Presenter`/`Output`;`SeatResponse(String id, String seatNumber, SeatStatus status)`;`ResponseContext.set(Response)` / `.get()`;`ErrorResponse` DTO(用于 OpenAPI schema);`AdminResource` 现有的 `@CookieParam("Authorization")` + `responseContext.get()` 范式;集成测试现有 `JerseyTest` 配置(已注册 `AdminResource` + `SeatAndRoomBinder`,已注入 `InMemorySeatRepo`)。`InMemorySeatRepo` 构造时预置 seat-1..seat-8(AVAILABLE, room-1)、seat-9..seat-12(room-2);`save(Seat)` 保留既有 id 直接 put。
- Produces: HTTP 端点 `PUT /api/admin/seats/{id}`;`UpdateSeatRequest(String status)` DTO。

- [ ] **Step 1: 写第一个失败集成测试(200 维护下线)**

在 `AdminResourceIntegrationTest` 中,`// --- create seat tests ---` 区块之后、`// --- delete room tests ---` 之前,新增注释头与第一个测试:

```java
    // --- update seat tests ---

    @Test
    void shouldReturn200WhenAdminMarksSeatMaintenance()
    {
        // InMemorySeatRepo 预置 seat-1 (A-1, AVAILABLE) 在 room-1
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("seat-1", body.get("id"));
        assertEquals("A-1", body.get("seatNumber"));
        assertEquals("MAINTENANCE", body.get("status"));
    }
```

在测试类顶部 import 区追加:

```java
import org.cleancoders.web.dto.admin.UpdateSeatRequest;
```

- [ ] **Step 2: 运行测试,确认失败**

Run: `mvn -pl WebApi -am test -Dtest=AdminResourceIntegrationTest#shouldReturn200WhenAdminMarksSeatMaintenance`
Expected: 编译失败 — `UpdateSeatRequest` 不存在(还无法编译)。

- [ ] **Step 3: 创建 `UpdateSeatRequest` DTO**

Create `WebApi/src/main/java/org/cleancoders/web/dto/admin/UpdateSeatRequest.java`:

```java
package org.cleancoders.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新座位状态请求")
public record UpdateSeatRequest(
        @Schema(description = "目标状态", example = "MAINTENANCE",
                allowableValues = {"AVAILABLE", "MAINTENANCE"})
        String status
)
{
}
```

- [ ] **Step 4: 在 `WebApiRoomPresenter` 实现 `UpdateSeatUseCase.Presenter`**

Modify `WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java`。

(a) 在 `implements` 列表追加 `UpdateSeatUseCase.Presenter`(该类已 `import org.cleancoders.seatandroom.usecase.*`,且已 `import ...Seat`、`...SeatResponse`、`java.util.Map`)。把第 29-36 行的 implements 块改为:

```java
@Singleton
public class WebApiRoomPresenter extends WebApiPresenter implements
        ListRoomsUseCase.Presenter,
        ListSeatsUseCase.Presenter,
        ManageRoomsUseCase.Presenter,
        UpdateRoomUseCase.Presenter,
        DeleteRoomUseCase.Presenter,
        ManageSeatsUseCase.Presenter,
        UpdateSeatUseCase.Presenter
{
```

> 注:`UpdateSeatUseCase` 由 `import org.cleancoders.seatandroom.usecase.*;` 覆盖,无需新增 import。需新增
`import org.cleancoders.seatandroom.domain.SeatStatus;`(presenter 方法用到 `SeatStatus` 参数,但仅作传递,实际不需要 ——
> 检查:方法签名 `invalidStatusTransition(String, SeatStatus, SeatStatus)` 需要类型可见)。**新增 import**:
`import org.cleancoders.seatandroom.domain.SeatStatus;`。

(b) 在文件末尾(现有 `seatNumberAlreadyExists` 方法之后、类结束 `}` 之前)追加:

```java

    // --- UpdateSeatUseCase (UC-07 update seat status) ---

    @Override
    public void updateSuccess(Seat seat)
    {
        responseContext.set(Response.ok(
                new SeatResponse(seat.id(), seat.seatNumber(), seat.status())
        ).build());
    }

    @Override
    public void seatNotFound(String seatId)
    {
        responseContext.set(Response.status(404).entity(Map.of(
                "error", "座位不存在",
                "seatId", seatId
        )).build());
    }

    @Override
    public void invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "非法状态转换",
                "seatId", seatId,
                "currentStatus", current.name(),
                "targetStatus", target.name()
        )).build());
    }

    @Override
    public void invalidStatus(String seatId, String status)
    {
        responseContext.set(Response.status(400).entity(Map.of(
                "error", "非法座位状态",
                "seatId", seatId,
                "status", status == null ? "null" : status
        )).build());
    }
```

> 注意:`updateSuccess` 与 `ManageSeatsUseCase.Presenter` 继承的 `success(Seat)` 不冲突 —— 后者签名是 `success(Seat)`(201),本接口是 `updateSuccess(Seat)`(200),方法名不同,二者共存(与 rooms 的 `success`/`updateSuccess` 并存模式一致)。

- [ ] **Step 5: 在 `SeatAndRoomBinder` 绑定 `UpdateSeatUseCase`**

Modify `WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java`。

(a) `bind(UpdateSeatUseCase.class).to(UpdateSeatUseCase.class);` —— 在 `bind(ManageSeatsUseCase.class)...` 行之后追加(use case 区块)。

(b) presenter 绑定:把现有
```java
        bind(WebApiRoomPresenter.class)
                .to(ListRoomsUseCase.Presenter.class)
                .to(ListSeatsUseCase.Presenter.class)
                .to(ManageRoomsUseCase.Presenter.class)
                .to(UpdateRoomUseCase.Presenter.class)
                .to(DeleteRoomUseCase.Presenter.class)
                .to(ManageSeatsUseCase.Presenter.class)
                .in(Singleton.class);
```
追加一行 `.to(UpdateSeatUseCase.Presenter.class)`(放在 `.to(ManageSeatsUseCase.Presenter.class)` 之后、`.in(Singleton.class)` 之前)。`UpdateSeatUseCase` 由现有 `import org.cleancoders.seatandroom.usecase.*;` 覆盖。

- [ ] **Step 6: 在 `AdminResource` 新增端点 + `@Inject`**

Modify `WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java`。

(a) import 区追加(在 `CreateSeatRequest` import 附近):
```java
import org.cleancoders.seatandroom.usecase.UpdateSeatUseCase;
```
```java
import org.cleancoders.web.dto.admin.UpdateSeatRequest;
```

(b) 在 `@Inject ManageSeatsUseCase manageSeatsUseCase;` 字段之后追加:
```java

    @Inject
    UpdateSeatUseCase updateSeatUseCase;
```

(c) 在 `createSeat(...)` 方法之后、`updateRoom(...)` 方法之前(即 POST `/seats` 与 PUT `/rooms/{id}` 之间),插入:

```java
    @PUT
    @Path("/seats/{id}")
    @Operation(summary = "更新座位状态 (UC-07)", description = "管理员切换座位状态(AVAILABLE↔MAINTENANCE)。座位不存在返回 404,非法状态值返回 400,非法转换返回 409。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SeatResponse.class))),
            @ApiResponse(responseCode = "400", description = "非法座位状态",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "座位不存在",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "非法状态转换",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateSeat(
            @CookieParam("Authorization") String authCookie,
            @Parameter(description = "座位ID", required = true, example = "seat-1")
            @PathParam("id") String seatId,
            UpdateSeatRequest input)
    {
        updateSeatUseCase.execute(new UpdateSeatUseCase.Request(
                authCookie, seatId, input.status()));
        return responseContext.get();
    }
```

- [ ] **Step 7: 运行第一个集成测试,确认通过**

Run: `mvn -pl WebApi -am test -Dtest=AdminResourceIntegrationTest#shouldReturn200WhenAdminMarksSeatMaintenance`
Expected: PASS。

- [ ] **Step 8: 暴露 `seatRepo` 字段(为后续需预置 MAINTENANCE/RESERVED 座位的测试做准备)**

现有 `AdminResourceIntegrationTest` 只暴露了 `roomRepo` 字段,未暴露 `seatRepo`。后续几个测试需直接向 repo 注入非 AVAILABLE 座位。

Modify `WebApi/src/test/java/org/cleancoders/web/resource/AdminResourceIntegrationTest.java`:

(a) 类字段区(第 42 行 `private InMemoryRoomRepo roomRepo;` 之下)追加:
```java
    private InMemorySeatRepo seatRepo;
```

(b) 在 `configure()` 中,把 `var seatRepo = new InMemorySeatRepo();` 改为赋值给字段:
```java
        seatRepo = new InMemorySeatRepo();
```
(其余 `bind(seatRepo).to(SeatRepository.class);` 行不变。)`InMemorySeatRepo` 的 import 已在第 14 行存在,无需新增。

- [ ] **Step 9: 追加其余 7 个集成测试**

在 `shouldReturn200WhenAdminMarksSeatMaintenance` 之后(仍在 `// --- update seat tests ---` 区块内)追加:

```java
    @Test
    void shouldReturn200WhenAdminMarksSeatAvailable()
    {
        seatRepo.save(new Seat("seat-m", "room-1", "A-9", SeatStatus.MAINTENANCE));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-m")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("AVAILABLE")));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("seat-m", body.get("id"));
        assertEquals("AVAILABLE", body.get("status"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonexistentSeat()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位不存在", body.get("error"));
        assertEquals("nonexistent", body.get("seatId"));
    }

    @Test
    void shouldReturn400WhenStatusInvalid()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("BROKEN")));

        assertEquals(400, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("非法座位状态", body.get("error"));
        assertEquals("BROKEN", body.get("status"));
    }

    @Test
    void shouldReturn400WhenStatusNotAdminControllable()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("RESERVED")));

        assertEquals(400, response.getStatus());
    }

    @Test
    void shouldReturn409WhenIllegalTransition()
    {
        seatRepo.save(new Seat("seat-r", "room-1", "A-9", SeatStatus.RESERVED));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-r")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("非法状态转换", body.get("error"));
        assertEquals("RESERVED", body.get("currentStatus"));
        assertEquals("MAINTENANCE", body.get("targetStatus"));
    }

    @Test
    void shouldReturn403WhenStudentUpdatesSeat()
    {
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn401WhenNoTokenForSeatUpdate()
    {
        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(401, response.getStatus());
    }
```

追加 import(在测试类 import 区):
```java
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
```

- [ ] **Step 10: 运行全部 seat-update 集成测试,确认通过**

Run: `mvn -pl WebApi -am test -Dtest=AdminResourceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS,新增 8 个 `should*Seat*` / `shouldReturn*Seat*` 测试全绿(既有测试不受影响)。

- [ ] **Step 11: 运行 WebApi 模块全量测试,确认无回归**

Run: `mvn -pl WebApi -am test`
Expected: 全绿(含既有 room/seat create、room update/delete 等)。

- [ ] **Step 12: 全量构建验证**

Run: `mvn -pl SeatAndRoom,WebApi -am test`
Expected: 全绿。

- [ ] **Step 13: 提交**

```bash
git add WebApi/src/main/java/org/cleancoders/web/dto/admin/UpdateSeatRequest.java \
        WebApi/src/main/java/org/cleancoders/web/presenter/WebApiRoomPresenter.java \
        WebApi/src/main/java/org/cleancoders/web/binder/SeatAndRoomBinder.java \
        WebApi/src/main/java/org/cleancoders/web/resource/AdminResource.java \
        WebApi/src/test/java/org/cleancoders/web/resource/AdminResourceIntegrationTest.java
git commit -m "feat(WebApi): AdminResource 新增 PUT /api/admin/seats/{id} (UC-07 更新座位)

- 新增 UpdateSeatRequest DTO (status 单字段)
- WebApiRoomPresenter 实现 UpdateSeatUseCase.Presenter
  (200 成功 / 404 座位不存在 / 400 非法状态 / 409 非法转换)
- SeatAndRoomBinder 绑定 UpdateSeatUseCase 及其 Presenter
- 8 个集成测试

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review 记录(实现前后对照)

- **Spec 覆盖**:方案 A(仅 status)✓;不靠异常控流 ✓;404/400/409/200 ✓;presenter 在 WebApiRoomPresenter ✓;binder 绑定 ✓;复用 StubSeatRepo ✓;6 单元 + 8 集成 ✓;`mvn -pl SeatAndRoom,WebApi -am test` ✓;不动 Seat / 不扩张 SeatRepository ✓。
- **类型一致性**:`Presenter.invalidStatusTransition(String, SeatStatus, SeatStatus)` 在 Task 1 定义、Task 2 实现,签名一致 ✓;`Request(String token, String seatId, String status)` 一致 ✓;`UpdateSeatRequest(String status)` 一致 ✓。
- **占位符**:Step 8 的占位测试块已显式标注"替换为真实代码"并提供真实代码,无残留 TBD。