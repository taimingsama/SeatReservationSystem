---
name: uc07-seat-crud-split-pattern
description: UC-07 seat CRUD follows UC-06's split pattern — separate use case class per operation (create/update/delete), not methods on one class
metadata:
  type: project
---

UC-06 rooms split CRUD into three independent use case classes: `ManageRoomsUseCase` (create="POST"),
`UpdateRoomUseCase` (update="PUT"), `DeleteRoomUseCase` (delete="DELETE"). The POST /api/admin/seats commit (fe764a4)
explicitly says `ManageSeatsUseCase` was created "对齐 UC-06 的拆分模式", so `ManageSeatsUseCase` currently only does *
*create**.

**Why:** This codebase's convention is one use-case class per operation, each with its own nested `Request`/`Output`/
`Presenter` interfaces, each presenter method backed by a branch in `WebApiRoomPresenter` (or `WebApiAdminPresenter`)
and bound in `SeatAndRoomBinder` (seats) / `AppBinder` (rooms).

**How to apply:** To add PUT /api/admin/seats/{id}, create a **new `UpdateSeatUseCase`** class extending
`AdminAuthUseCase` — do NOT add an update method to `ManageSeatsUseCase`. Mirror `UpdateRoomUseCase`'s structure (
findById → business checks → save → presenter.updateSuccess). Wire presenter + binder, add unit test
`UpdateSeatUseCaseTest` and integration tests in `AdminResourceIntegrationTest`. Related: [[seat-domain-mutability]].