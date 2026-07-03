# UC-07 DeleteSeatUseCase 实现设计

> 端点:`DELETE /api/admin/seats/{id}` — 删除座位（管理员，软删除）
> 权限:管理员(继承 `AdminAuthUseCase`,401/403 由基类处理)
> 模块归属:SeatAndRoom

## 背景与现状

- UC-07 定义(`用例清单.md`):"座位 CRUD + 状态切换(含维护)",Actor 为管理员。
- 已落地:
  - POST /api/admin/seats(commit `fe764a4`):`ManageSeatsUseCase` 仅承载创建。
  - PUT /api/admin/seats(commit `f21e4ea` / `18056ad`):`UpdateSeatUseCase` 独立类,管理员切换 AVAILABLE↔MAINTENANCE。
- UC-06 的拆分模式(commit `f7e9ad8` `DeleteRoomUseCase`):create/update/delete 各为独立 UseCase 类,各有独立 `Request`/`Output`/`Presenter`。本设计新增 `DeleteSeatUseCase` 与之对齐,不向 `ManageSeatsUseCase` 加方法。

### 关键不对称(本设计要决定的根因)

- **房间删除是软删除**:`StudyRoom` 有 `CLOSED` 状态,`DeleteRoomUseCase` 把 OPEN/MAINTENANCE 转 CLOSED,`findByStatus(OPEN)` 对学生隐藏;已 CLOSED 再删返回 409(`roomAlreadyClosed`)。
- **座位删除没有等价状态**:`SeatStatus` 仅 `AVAILABLE`/`RESERVED`/`OCCUPIED`/`MAINTENANCE`;`MAINTENANCE` 语义是"临时维护",不是"删除"。`SeatRepository` 也无 `delete` 方法(仅 `findById`/`save`/`findByRoomId`)。

## 决策:删除语义

采用**软删除——新增 `REMOVED` 状态**。理由:

- 与 `DeleteRoomUseCase`(CLOSED 软删)对称:数据保留,可恢复,列表展示可后续过滤。
- `MAINTENANCE` 保留原语义("临时下线"),与"删除"区分,不混淆。
- 不引入硬删除(数据完整性风险最高、与项目一贯软删风格相悖)。

## 决策:有活跃预约时拒绝

>DeleteSeatUseCase 不允许删除存在活跃预约的座位——见 Section 4 活跃预约检查。这是用户明确的决策 B。

## 设计

### Section 1 — 模块桥接:`ActiveReservationChecker` 窄口

决策 B 要求 DeleteSeatUseCase(在 SeatAndRoom)查询活跃预约,但 SeatAndRoom **当前不依赖 Reservation 模块**,`ReservationRepository`/`ReservationStatus` 都在 Reservation 模块内(不在公共模块)。

采用**在已有公共模块加窄口**方案:

- 新建单方法接口 `ActiveReservationChecker`,放 `Common_Reservation_SeatAndRoom/outbound`(与 `SeatRepository` 同位同包)。
- `DeleteSeatUseCase` 只依赖这个窄口,**完全不碰** `Reservation`/`ReservationStatus`/`ReservationRepository`。
- 实现放 Infrastructure(已依赖 Reservation 与 Common_Reservation_SeatAndRoom 模块)。
- "活跃 = RESERVED ∪ CHECKED_IN"的定义封装在实现里,不泄漏进 SeatAndRoom。

> **已核实的依赖事实**:`InMemorySeatRepo` 引用 `common_reservation_seatAndRoom.outbound.SeatRepository`,证明 Infrastructure 已能解析 `Common_Reservation_SeatAndRoom` 模块类型(经传递依赖 Infrastructure→SeatAndRoom→Common_Reservation_SeatAndRoom)。`ActiveReservationChecker` 接口放该公共模块、实现放 Infrastructure 注入它——无需新增 pom 依赖。

### Section 2 — 领域模型改动

①`SeatStatus` 枚举新增 `REMOVED`(`Common_Reservation_SeatAndRoom/domain/SeatStatus.java`):
```java
public enum SeatStatus {
    AVAILABLE, RESERVED, OCCUPIED, MAINTENANCE, REMOVED
}
```

②`Seat` 新增域方法 `markRemoved()`(`Common_Reservation_SeatAndRoom/domain/Seat.java`):
```java
/** 软删除:仅 AVAILABLE 或 MAINTENANCE 可转 REMOVED。 */
public void markRemoved() {
    if (status != SeatStatus.AVAILABLE && status != SeatStatus.MAINTENANCE) {
        throw new IllegalStateException("只有可用或维护中的座位才能删除，当前状态: " + status);
    }
    this.status = SeatStatus.REMOVED;
}
```
- 与现有 `markMaintenance()`/`markAvailable()` 风格一致(检查前置 + 抛 `IllegalStateException`)。
- `RESERVED`/`OCCUPIED` 一律不得软删(UseCase 层域方法双重保险)。
- `markAvailable()` 维持不变(`MAINTENANCE → AVAILABLE`),不与 `REMOVED` 互通——软删后恢复需走专门路径,**不在本特性范围**(YAGNI)。

③ 展示过滤:**本特性不顺带改 ListSeats 展示过滤**——`ListSeatsUseCase`/`WebApiRoomPresenter.presentSeats` 是否展示 REMOVED 是展示语义,属另一用例职责;混进本特性会扩到学生侧查询路径,超出最小化原则。Room 删除(CLOSED)当时是否在 ListRooms 做过滤未核实——若要自洽应作为单独项处理。

### Section 3 — UseCase 层:`DeleteSeatUseCase`

`SeatAndRoom/.../seatandroom/usecase/DeleteSeatUseCase.java`,对标 `DeleteRoomUseCase`,继承 `AdminAuthUseCase`:

```java
public class DeleteSeatUseCase
        extends AdminAuthUseCase<DeleteSeatUseCase.Request, DeleteSeatUseCase.Output>
{
    @Inject Presenter presenter;
    @Inject SeatRepository seatRepo;
    @Inject ActiveReservationChecker activeReservationChecker;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = seatRepo.findById(req.seatId());
        if (existing.isEmpty()) {
            presenter.seatNotFound(req.seatId());
            return null;
        }

        Seat seat = existing.get();
        if (seat.status() == SeatStatus.REMOVED) {
            presenter.seatAlreadyRemoved(req.seatId());
            return null;
        }

        // 域级保护:RESERVED/OCCUPIED 不得删
        if (seat.status() != SeatStatus.AVAILABLE
                && seat.status() != SeatStatus.MAINTENANCE) {
            presenter.seatInUse(req.seatId(), seat.status());
            return null;
        }

        // 活跃预约检查(决策 B):有 RESERVED/CHECKED_IN 预约则拒绝
        if (activeReservationChecker.hasActiveForSeat(req.seatId())) {
            presenter.seatHasActiveReservations(req.seatId());
            return null;
        }

        seat.markRemoved();
        Seat saved = seatRepo.save(seat);
        presenter.deleteSuccess(saved.id());
        return new Output(saved.id());
    }

    public interface Presenter {
        void deleteSuccess(String seatId);
        void seatNotFound(String seatId);
        void seatAlreadyRemoved(String seatId);
        void seatInUse(String seatId, SeatStatus current);
        void seatHasActiveReservations(String seatId);
    }

    public record Request(String token, String seatId) implements AuthUseCase.Request {}
    public record Output(String seatId) {}
}
```

要点:
- **不靠异常控流**:先比对 `seat.status()`,只在合法时调 `markRemoved()`——与 `UpdateSeatUseCase` 的检查+调用风格一致,不依赖 `markRemoved()` 抛异常做分支。
- **三道前置检查 + 一道软删**:
  - 不存在 → `seatNotFound`(404)
  - 已 REMOVED → `seatAlreadyRemoved`(409),与 `DeleteRoomUseCase.roomAlreadyClosed` 对称
  - RESERVED/OCCUPIED → `seatInUse`(409),域级保护
  - 有活跃预约 → `seatHasActiveReservations`(409),查预约表里的 RESERVED/CHECKED_IN
  - 通过 → `markRemoved()`+`save()`+`deleteSuccess`(200)
- **域保护与预约检查分工**:域方法把 RESERVED/OCCUPIED 顶在门外;但 AVAILABLE 座位仍可能挂未来时段的 RESERVED 预约(座位状态机制粒度限制),`ActiveReservationChecker` 查预约表兜这个口子。

### Section 4 — 活跃预约检查的实现

#### 4.1 窄口接口

`Common_Reservation_SeatAndRoom/outbound/ActiveReservationChecker.java`:
```java
package org.cleancoders.common_reservation_seatAndRoom.outbound;

/** 座位删除前的活跃预约检查(决策 B 窄口,与 SeatRepository 同位)。 */
public interface ActiveReservationChecker {
    /** 该座位是否存在活跃预约(RESERVED 或 CHECKED_IN)。 */
    boolean hasActiveForSeat(String seatId);
}
```
- 单方法,只表达"做什么",参数/返回值用基本类型,**完全不暴露** `Reservation`/`ReservationStatus`/`ReservationRepository`。
- "活跃 = RESERVED ∪ CHECKED_IN"封装在实现里,不泄漏进 SeatAndRoom 模块。`CANCELLED`/`CHECKED_OUT`/`EXPIRED` 不视为活跃。

#### 4.2 ReservationRepository 新增仓储方法

`ReservationRepository` 新增(扩接口,波及 2 个实现 + 测试桩):
```java
List<Reservation> findBySeatIdAndStatusIn(String seatId, Set<ReservationStatus> statuses);
```
- 返回 `List<Reservation>`,调用方用 `!isEmpty()` 判断;比返回 `boolean` 更通用,后续恢复用例/审计可复用。
- 与现有 `findBySeatIdAndDateAndTimeSlotIdAndStatusIn` 命名同构(去掉 date/timeSlot 两维度)。
- **受波及实现(2 个,各加 stream filter 实现)**:
  - `Infrastructure/.../InMemoryReservationRepo.java`
  - `Reservation/src/test/.../StubReservationRepo.java`(4 个预约用例测试的共享桩;加默认实现返回空 List,不影响既有用例)
- 其余 6 个用例 + 4 个测试只 import 接口、不实现,**不受影响**。

#### 4.3 Infrastructure 实现

`Infrastructure/.../persistence/ReservationBasedActiveReservationChecker.java`:
```java
package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.reservation.domain.ReservationStatus;

import java.util.Set;

@Singleton
public class ReservationBasedActiveReservationChecker implements ActiveReservationChecker {
    @Inject ReservationRepository reservationRepo;  // 已绑定的现有实现

    @Override
    public boolean hasActiveForSeat(String seatId) {
        return !reservationRepo.findBySeatIdAndStatusIn(seatId,
                Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN)).isEmpty();
    }
}
```
- Infrastructure 已依赖 Reservation 模块与 `Common_Reservation_SeatAndRoom`(经 SeatAndRoom 传递),注入 `ReservationRepository` 无新模块边。
- "活跃"定义(RESERVED ∪ CHECKED_IN)只在此处出现,SeatAndRoom 不可见。

### Section 5 — WebApi 层

#### Presenter — `WebApiRoomPresenter` 新增 `implements DeleteSeatUseCase.Presenter`

与 `UpdateSeatUseCase.Presenter` 并列:

| Presenter 方法 | HTTP | Body |
|---|---|---|
| `deleteSuccess(seatId)` | 200 | `{message:"座位已删除", seatId}` |
| `seatNotFound(seatId)` | 404 | `{error:"座位不存在", seatId}` |
| `seatAlreadyRemoved(seatId)` | 409 | `{error:"座位已处于删除状态", seatId}` |
| `seatInUse(seatId, current)` | 409 | `{error:"座位正在使用中，无法删除", seatId, currentStatus: current.name()}` |
| `seatHasActiveReservations(seatId)` | 409 | `{error:"座位存在活跃预约，无法删除", seatId}` |

- 沿用 `responseContext.set(...)`,与 `DeleteRoomUseCase` 的 `deleteSuccess`/`roomAlreadyClosed` 一致。

#### Resource — `AdminResource` 新增端点

紧跟 `updateSeat` 之后,与 `deleteRoom` 风格对称:
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
- `AdminResource` 顶部 `@Inject DeleteSeatUseCase deleteSeatUseCase;`。
- `@CookieParam("Authorization")` + `responseContext.get()`,与现有 createSeat/updateSeat/deleteRoom 完全一致。

#### Binder — `SeatAndRoomBinder.configure()` 补三处

```java
// UseCase
bind(DeleteSeatUseCase.class).to(DeleteSeatUseCase.class);
// Presenter 绑定追加一行(在 WebApiRoomPresenter 的 .to(...) 链尾)
//   .to(DeleteSeatUseCase.Presenter.class)
// ActiveReservationChecker → Infrastructure 实现
bind(ReservationBasedActiveReservationChecker.class)
    .to(ActiveReservationChecker.class).in(Singleton.class);
```

### 错误处理一览

| 场景 | HTTP | 触发 |
|---|---|---|
| 无 token / token 失效 | 401 | `WebApiAuthPresenter.invalidToken` |
| 非管理员 | 403 | `WebApiAuthPresenter.forbidden` |
| 座位不存在 | 404 | `presenter.seatNotFound` |
| 座位已 REMOVED | 409 | `presenter.seatAlreadyRemoved` |
| 座位 RESERVED/OCCUPIED | 409 | `presenter.seatInUse` |
| 有活跃预约(RESERVED/CHECKED_IN) | 409 | `presenter.seatHasActiveReservations` |
| 成功软删 | 200 | `presenter.deleteSuccess` |

> 本用例**无 400 分支**——DELETE 无请求体,不存在参数校验失败(与 `deleteRoom` 一致,它也只在 404/409)。

## 测试设计

### 受波及的已有测试(回归检查)

新增 `ReservationRepository.findBySeatIdAndStatusIn` 扩张接口,2 个实现各加方法。`StubReservationRepo` 补默认实现(返回空 List),**确保现有 Reservation 模块共享该桩的 4 个用例测试(CancelReservation/CheckOut/ListMyReservations/ManageReservations)仍通过**。spec 执行期跑 `mvn -pl Reservation -am test` 验证零回归。

### 单元测试 `DeleteSeatUseCaseTest`(SeatAndRoom,对标 `DeleteRoomUseCaseTest`)

复用 `StubTokenService`/`StubUserRepo`/`StubSeatRepo`(现存,见 `Common_Reservation_SeatAndRoom_Test_Infrastructure`) + `StubActiveReservationChecker`(新建,单方法可匿名实现)。

注入方式对标 `DeleteRoomUseCaseTest`:直接 `new DeleteSeatUseCase()` + 直接赋值公开字段 `tokenService`/`userRepo`/`presenter`/`seatRepo`/`activeReservationChecker`(与 `.tokenService=` `.roomRepo=` 等同法,非 @Inject 反射注入)。

用例:
1. `shouldRemoveAvailableSeat` — AVAILABLE 座位无活跃预约 → `deleteSuccessCalled`,状态断言 REMOVED。
2. `shouldRemoveMaintenanceSeat` — MAINTENANCE 座位 → 200。
3. `shouldReturnSeatNotFound` — 不存在 id → `seatNotFoundCalled`,404 路径。
4. `shouldReturnSeatAlreadyRemoved` — 预置 REMOVED 座位 → `seatAlreadyRemovedCalled`。
5. `shouldRejectReservedSeat` — 预置 RESERVED 座位 → `seatInUseCalled`,带 RESERVED。
6. `shouldRejectOccupiedSeat` — 预置 OCCUPIED 座位 → `seatInUseCalled`,带 OCCUPIED。
7. `shouldRejectWhenActiveReservationsExist` — AVAILABLE 座位 + `activeReservationChecker` 返回 true → `seatHasActiveReservationsCalled`(即使座位状态可删,仍被预约检查拦下)。
8. `shouldAllowWhenOnlyHistoricalReservations` — `activeReservationChecker` 返回 false → 正常软删(验证只查活跃,不误伤 CANCELLED/EXPIRED)。

`StubPresenter` 需实现 `DeleteSeatUseCase.Presenter` + `AdminAuthUseCase.Presenter` + `AuthUseCase.Presenter`。

### Infrastructure 测试 `ReservationBasedActiveReservationCheckerTest`

注入含预约的 `StubReservationRepo` 或 `InMemoryReservationRepo`:
- `shouldReturnTrueWhenReservedExists` — 含 RESERVED 预约 → true。
- `shouldReturnTrueWhenCheckedInExists` — 含 CHECKED_IN 预约 → true。
- `shouldReturnFalseWhenAllCancelledOrExpired` — 仅 CANCELLED/EXPIRED → false。
- `shouldReturnFalseWhenNoReservations` — 无预约 → false。

### 集成测试(`AdminResourceIntegrationTest`,在 update-seat 测试之后追加)

- `shouldReturn200WhenAdminDeletesAvailableSeat` — 预置 AVAILABLE 座位,无活跃预约 → DELETE → 200, body `message="座位已删除"`。
- `shouldReturn200WhenAdminDeletesMaintenanceSeat` → 200。
- `shouldReturn404WhenDeletingNonexistentSeat` → 404。
- `shouldReturn409WhenSeatAlreadyRemoved` — 预置 REMOVED → DELETE → 409。
- `shouldReturn409WhenSeatReserved` — 预置 RESERVED → 409, `currentStatus=RESERVED`。
- `shouldReturn409WhenSeatHasActiveReservations` — AVAILABLE 座位挂一条 RESERVED 预约 → 409。
- `shouldReturn200WhenSeatOnlyHasCancelledReservations` — AVAILABLE 座位 + 仅 CANCELLED 预约 → 200。
- `shouldReturn403WhenStudentDeletesSeat` — 学生 token → 403。
- `shouldReturn401WhenNoTokenForSeatDelete` — 不带 cookie → 401。

### 构建验证

`mvn -pl SeatAndRoom,WebApi,Infrastructure,Common_Reservation_SeatAndRoom -am test` 全绿(覆盖领域改动 + 新接口实现 + 端到端)。

## 文件清单

```
Common_Reservation_SeatAndRoom/domain/SeatStatus.java          ← 改:加 REMOVED
Common_Reservation_SeatAndRoom/domain/Seat.java                 ← 改:加 markRemoved()
Common_Reservation_SeatAndRoom/outbound/ActiveReservationChecker.java   ← 新:窄口接口

SeatAndRoom/usecase/DeleteSeatUseCase.java                     ← 新:UseCase

Reservation/outbound/ReservationRepository.java                 ← 改:加 findBySeatIdAndStatusIn
Infrastructure/persistence/InMemoryReservationRepo.java        ← 改:实现新方法
Reservation/src/test/.../StubReservationRepo.java               ← 改:补默认实现
Infrastructure/persistence/ReservationBasedActiveReservationChecker.java  ← 新:窄口实现

WebApi/presenter/WebApiRoomPresenter.java                      ← 改:implements DeleteSeatUseCase.Presenter + 5 方法
WebApi/resource/AdminResource.java                             ← 改:加 DELETE 端点 + @Inject
WebApi/binder/SeatAndRoomBinder.java                           ← 改:3 处绑定
```

## 影响面与不变性

- **领域**:新增 `REMOVED` 枚举值 + `markRemoved()` 域方法;`Seat` 其他域方法不变,`SeatRepository` 接口不扩张。
- **Reservation 仓储**:扩张 `findBySeatIdAndStatusIn`(波及 2 实现 + 测试桩),其余用例/测试零改动。
- **桥接**:新增 `ActiveReservationChecker` 窄口,SeatAndRoom 不反向依赖 Reservation 模块,无新模块边。
- **WebApi**:`DeleteSeatUseCase` 与 UC-06/POST-seats/PUT-seats 拆分模式对齐;`ManageSeatsUseCase` 保持只读创建职责;`WebApiRoomPresenter` 加分支,`WebApiAdminPresenter` 不动。

## 待澄清项(后续按需收紧,不在本特性实现)

- `ListSeatsUseCase` / `WebApiRoomPresenter.presentSeats` 是否过滤 REMOVED:本特性不改;若需对学生隐藏 REMOVED 座位,作为单独一项处理(并核实 Room-CLOSED 是否在 ListRooms 已过滤以保持自洽)。
- REMOVED → 可用 的恢复路径(若产品需要),不在本特性范围。