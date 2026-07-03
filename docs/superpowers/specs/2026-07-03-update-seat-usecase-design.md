# UC-07 UpdateSeatUseCase 实现设计

> 端点:`PUT /api/admin/seats/{id}` — 更新座位状态（管理员）
> 权限:管理员(继承 `AdminAuthUseCase`,401/403/404 由基类处理)
> 模块归属:SeatAndRoom

## 背景与现状

- UC-07 定义(`用例清单.md`):"座位 CRUD + 状态切换(含维护)",Actor 为管理员。
- POST /api/admin/seats(commit `fe764a4`)已落地创建侧:`ManageSeatsUseCase` + `CreateSeatRequest` + `WebApiRoomPresenter`(实现 `ManageSeatsUseCase.Presenter`)+ `AdminResource` 端点 + `SeatAndRoomBinder` 绑定。其提交说明明确"对齐 UC-06 的拆分模式"——`ManageSeatsUseCase` 当前**仅承载创建**。
- UC-06 的拆分模式(commit `985b5b5`):create/update/delete 各为一个独立 UseCase 类(`ManageRoomsUseCase` / `UpdateRoomUseCase` / `DeleteRoomUseCase`),各有独立 `Request`/`Output`/`Presenter`。本设计新增 `UpdateSeatUseCase` 与之对齐,不向 `ManageSeatsUseCase` 加方法。
- **关键领域约束**:`Seat`(`Common_Reservation_SeatAndRoom` 模块)中 `roomId`、`seatNumber` 为 `final`(无 setter),仅 `id`、`status` 可变。管理员可见的状态转换域方法仅有:
  - `markMaintenance()`:`AVAILABLE` → `MAINTENANCE`,否则抛 `IllegalStateException`。
  - `markAvailable()`:`MAINTENANCE` → `AVAILABLE`,否则抛 `IllegalStateException`。
  - `reserve()/release()/occupy()` 属预约用例(UC-08/09/10),**不属于管理员更新**。
- 现有 `SeatRepository` 已具备 `findById` / `save` / `findByRoomId`,接口足够,**无需扩张**。

## 设计目标

"更新座位"在领域不变性约束下,语义对应 UC-07 中"状态切换(含维护)"那一半——把故障座位下线维护、修好后恢复上线。请求体只含 `status`。

## 决策:请求体范围

采用**方案 A:仅改 status**。理由:

- 尊重 `Seat` 的领域不变性(`roomId`/`seatNumber` 不可变),零领域模型改动。
- 精确覆盖 UC-07 "状态切换(含维护)"语义,管理员通过本接口做维护上下线。
- `status` 取值限定为 `MAINTENANCE` / `AVAILABLE`(管理员可控的两态)。`RESERVED`/`OCCUPIED` 由预约流程流转,不接受通过本接口设置。
- 不选方案 B(改编号)/C(全量替换):两者需破坏 `Seat` 不可变性、重做编号唯一性校验,且对已预约座位改编号有数据一致性风险,与现有领域模型相悖。

## 设计

### 1. DTO 层 — 新增 `UpdateSeatRequest`

`WebApi/.../web/dto/admin/UpdateSeatRequest.java`,与 `CreateSeatRequest` 并列:

```java
@Schema(description = "更新座位请求")
public record UpdateSeatRequest(
        @Schema(description = "目标状态", example = "MAINTENANCE",
                allowableValues = {"AVAILABLE", "MAINTENANCE"})
        String status
) {}
```

- 仅 `status` 一个字段(`seatId` 取自路径参数,不进 body)。
- `allowableValues` 给 OpenAPI 文档约束;非法值由 UseCase 在运行期校验并返回 400。

### 2. UseCase 层 — 新增 `UpdateSeatUseCase`

`SeatAndRoom/.../seatandroom/usecase/UpdateSeatUseCase.java`,对标 `UpdateRoomUseCase` 继承 `AdminAuthUseCase`:

```java
public class UpdateSeatUseCase
        extends AdminAuthUseCase<UpdateSeatUseCase.Request, UpdateSeatUseCase.Output>
{
    @Inject Presenter presenter;
    @Inject SeatRepository seatRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = seatRepo.findById(req.seatId());
        if (existing.isEmpty()) {
            presenter.seatNotFound(req.seatId());
            return null;
        }

        Seat seat = existing.get();
        if (req.status() == null) {
            presenter.invalidStatus(req.seatId(), null);
            return null;
        }
        SeatStatus target;
        try {
            target = SeatStatus.valueOf(req.status());
        } catch (IllegalArgumentException e) {
            presenter.invalidStatus(req.seatId(), req.status());
            return null;
        }

        // 仅允许管理员可控两态;RESERVED/OCCUPIED 不接受
        if (target != SeatStatus.AVAILABLE && target != SeatStatus.MAINTENANCE) {
            presenter.invalidStatus(req.seatId(), req.status());
            return null;
        }

        // 不靠异常控流:先检查当前状态,只调合法路径
        SeatStatus current = seat.status();
        if (target == SeatStatus.MAINTENANCE) {
            if (current != SeatStatus.AVAILABLE) {
                presenter.invalidStatusTransition(req.seatId(), current, target);
                return null;
            }
            seat.markMaintenance();
        } else { // AVAILABLE
            if (current != SeatStatus.MAINTENANCE) {
                presenter.invalidStatusTransition(req.seatId(), current, target);
                return null;
            }
            seat.markAvailable();
        }

        Seat saved = seatRepo.save(seat);
        presenter.updateSuccess(saved);
        return new Output(saved.id());
    }

    public interface Presenter {
        void updateSuccess(Seat seat);
        void seatNotFound(String seatId);
        void invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target);
        void invalidStatus(String seatId, String status);
    }

    public record Request(String token, String seatId, String status) implements AuthUseCase.Request {}
    public record Output(String seatId) {}
}
```

要点:
- **不靠异常控流**:先比对 `seat.status()` 再调 `markMaintenance/markAvailable`,与 `Seat.reserve()` 等"检查+调用"风格一致;域方法本身抛 `IllegalStateException` 的行为不被依赖。
- 非法 `status` 字符串(`valueOf` 抛 `IllegalArgumentException`)、合法枚举但非管理员可控两态(`RESERVED`/`OCCUPIED`)——都走 `invalidStatus` (400)。
- 合法两态但当前状态不满足转换前提(如 RESERVED→MAINTENANCE)——走 `invalidStatusTransition` (409),回传 `current`/`target` 便于排错。

### 3. Presenter 层 — `WebApiRoomPresenter` 新增分支

在 `WebApiRoomPresenter` 上 `implements UpdateSeatUseCase.Presenter`,与 `ManageSeatsUseCase.Presenter` 并列(沿用 POST seats 选择该 presenter 的做法):

| Presenter 方法 | HTTP | Body |
|---|---|---|
| `updateSuccess(Seat)` | 200 | `SeatResponse(id, seatNumber, status)` |
| `seatNotFound(String seatId)` | 404 | `{error:"座位不存在", seatId}` |
| `invalidStatusTransition(seatId, current, target)` | 409 | `{error:"非法状态转换", seatId, currentStatus, targetStatus}` |
| `invalidStatus(seatId, status)` | 400 | `{error:"非法座位状态", seatId, status}` |

> 认证分支(401/403/404-user)由基类 `WebApiAuthPresenter` 经 `AdminAuthUseCase.Presenter` 注入,不在本接口内重复。

### 4. Resource 层 — `AdminResource` 新增端点

```java
@PUT
@Path("/seats/{id}")
@Operation(summary = "更新座位状态 (UC-07)",
        description = "管理员切换座位状态(AVAILABLE↔MAINTENANCE)。座位不存在返回 404,非法状态值返回 400,非法转换返回 409。")
@ApiResponses({
    @ApiResponse(responseCode = "200", ..., schema = @Schema(implementation = SeatResponse.class)),
    @ApiResponse(responseCode = "400", ..., schema = @Schema(implementation = ErrorResponse.class)),
    @ApiResponse(responseCode = "401", ...),
    @ApiResponse(responseCode = "403", ...),
    @ApiResponse(responseCode = "404", ...),
    @ApiResponse(responseCode = "409", ...),
})
public Response updateSeat(
        @CookieParam("Authorization") String authCookie,
        @PathParam("id") String seatId,
        UpdateSeatRequest input)
{
    updateSeatUseCase.execute(new UpdateSeatUseCase.Request(
            authCookie, seatId, input.status()));
    return responseContext.get();
}
```

- 在 `AdminResource` 中 `@Inject UpdateSeatUseCase updateSeatUseCase;`。
- `@CookieParam("Authorization")` + `responseContext.get()` 与现有 createSeat/updateRoom 完全一致。

### 5. Binder 层 — `SeatAndRoomBinder`

`SeatAndRoomBinder.configure()` 内补两行(对标 `UpdateRoomUseCase` 的绑定):

```java
bind(UpdateSeatUseCase.class).to(UpdateSeatUseCase.class);
// presenter 绑定追加:
//   .to(UpdateSeatUseCase.Presenter.class)
```

`WebApiRoomPresenter` 已是 `@Singleton`,沿用。

## 错误处理一览

| 场景 | HTTP | 触发 |
|---|---|---|
| 无 token / token 失效 | 401 | `WebApiAuthPresenter.invalidToken` |
| 非管理员 | 403 | `WebApiAuthPresenter.forbidden` |
| 座位不存在 | 404 | `presenter.seatNotFound` |
| `status` 为空 / 非法字符串 / RESERVED / OCCUPIED | 400 | `presenter.invalidStatus` |
| 合法两态但当前状态不满足转换前提 | 409 | `presenter.invalidStatusTransition` |
| 成功切换 | 200 | `presenter.updateSuccess` |

## 测试设计

### 单元测试 `UpdateSeatUseCaseTest`(SeatAndRoom,对标 `UpdateRoomUseCaseTest`)

复用 `StubTokenService` / `StubUserRepo` + 新建 `StubSeatRepo`(或检查是否已有可复用 stub)。用例:

1. `shouldMarkMaintenanceFromAvailable` — AVAILABLE→MAINTENANCE,200,`updateSuccessCalled`,status 断言。
2. `shouldMarkAvailableFromMaintenance` — MAINTENANCE→AVAILABLE,200。
3. `shouldReturnSeatNotFound` — 不存在 id → `seatNotFoundCalled`,404 路径。
4. `shouldRejectInvalidStatusTransition` — RESERVED→MAINTENANCE → `invalidStatusTransitionCalled`,带回 current/target。
5. `shouldRejectInvalidStatusValue` — `status="BROKEN"` → `invalidStatusCalled`;并补 `status="RESERVED"` 也走 400(管理员不可控)。
6. `shouldRejectNonAdminUser` — 学生 token → `forbiddenCalled`。

`StubPresenter` 需同时实现 `UpdateSeatUseCase.Presenter` + `AdminAuthUseCase.Presenter` + `AuthUseCase.Presenter`。

### 集成测试(`AdminResourceIntegrationTest`,在 create-seat 测试之后追加)

- `shouldReturn200WhenAdminMarksMaintenance` — 预置 AVAILABLE 座位 → PUT → 200, body `status=MAINTENANCE`。
- `shouldReturn200WhenAdminMarksAvailable` — 预置 MAINTENANCE 座位 → PUT → 200, body `status=AVAILABLE`。
- `shouldReturn404WhenUpdatingNonexistentSeat` — 不存在 id → 404, `error="座位不存在"`。
- `shouldReturn400WhenStatusInvalid` — `status="BROKEN"` → 400。
- `shouldReturn400WhenStatusNotAdminControllable` — `status="RESERVED"` → 400。
- `shouldReturn409WhenIllegalTransition` — 预置 RESERVED 座位(InMemorySeatRepo 支持直接构造)→ PUT `MAINTENANCE` → 409。
- `shouldReturn403WhenStudentUpdatesSeat` — 学生 token → 403。
- `shouldReturn401WhenNoTokenForSeatUpdate` — 不带 cookie → 401。

### 构建验证

`mvn -pl SeatAndRoom,WebApi -am test` 通过(与 POST seats commit 一致)。

## 影响面与不变性

- **不修改 `Seat` 领域模型**(复用既有 `markMaintenance`/`markAvailable`);不动 `seatNumber`/`roomId`。
- **不扩张 `SeatRepository`** 接口(`findById`+`save` 足够)——对 5 个 repo 实现(InMemory/Stub/TestData 等)零波及。
- 新增独立 `UpdateSeatUseCase`,与 UC-06/POST-seats 拆分模式对齐,`ManageSeatsUseCase` 保持只读创建职责。
- Presenter impl 加在 `WebApiRoomPresenter`(与 POST seats 一致);`WebApiAdminPresenter` 不动。

## 已确认项

- **seat stub 复用**:`Common_Reservation_SeatAndRoom_Test_Infrastructure` 模块已有 `StubSeatRepo`,实现 `SeatRepository`,提供 `findById` / `save` / `findByRoomId` 及种子方法 `addSeat(Seat)`。`ManageSeatsUseCaseTest` 已通过 `import ...StubSeatRepo` + `seatRepo = new StubSeatRepo()` + `addSeat(...)` 使用。`UpdateSeatUseCaseTest` 直接复用同一 stub(`addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE))` 预置座位),**无需新增任何测试基础设施**。