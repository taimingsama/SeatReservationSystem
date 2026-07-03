# 座位与教室重新设计

## 概述

用**布局模板 + 自动生成座位**模型替代现有的独立座位管理，去掉单独增删座位功能，座位生命周期完全跟随教室。

## 核心设计决策

| 决策 | 选择 |
|---|---|
| 布局管理 | 系统预设枚举（管理员不可增删） |
| 座位生成 | 教室创建时根据布局自动批量生成 |
| 座位标识 | int 序号（1..N），与 roomId 组成复合键 |
| 座位名称 | 不需要 |
| 布局变更 | 教室创建后不可更改布局 |
| 布局删除影响 | N/A（系统预设，不可删除） |
| 座位状态管理 | 保留（AVAILABLE ↔ MAINTENANCE 切换） |
| 座位单独增删 | 移除 |

## 一、领域模型

### RoomLayout（新增）

```java
public enum RoomLayout {
    SMALL("小教室", 40),      // 座位 1-40
    MEDIUM("中教室", 60),     // 座位 1-60
    LARGE("大教室", 100);     // 座位 1-100

    private final String displayName;
    private final int seatCount;
}
```

系统预设，后续可按需增减枚举值。

### StudyRoom（修改）

```java
// 之前
public record StudyRoom(String id, String name, String location, int capacity, RoomStatus status) {}

// 之后：capacity → layout
public record StudyRoom(String id, String name, String location, RoomLayout layout, RoomStatus status) {}
```

### Seat（修改）

```java
// 之前
public class Seat {
    private String id;        // UUID
    private String roomId;
    private String seatNumber; // "A-9"
    private SeatStatus status;
}

// 之后：id 改为 int，去掉 seatNumber
public class Seat {
    private int id;            // 教室内序号 1..N
    private String roomId;
    private SeatStatus status;
}
```

- 定位一个座位：`(roomId, seatId)` — seatId 是 1 到 N 的整数
- SeatStatus 枚举不变：AVAILABLE / RESERVED / OCCUPIED / MAINTENANCE / REMOVED

## 二、仓储层变更

### SeatRepository

```java
public interface SeatRepository {
    Optional<Seat> findByRoomIdAndSeatId(String roomId, int seatId);  // 替代 findById
    Seat save(Seat seat);
    List<Seat> findByRoomId(String roomId);
    List<Seat> findAll();
    void deleteByRoomId(String roomId);                               // 级联删除
}
```

- `findById(String)` → `findByRoomIdAndSeatId(String, int)`
- 新增 `deleteByRoomId` 支持删除教室时清理座位

### RoomRepository

接口不变。

## 三、用例变更

| 现有用例 | 操作 |
|---|---|
| `ManageRoomsUseCase` | **改** — 创建时选 layout，自动生成 N 个 Seat（status=AVAILABLE） |
| `DeleteRoomUseCase` | **改** — 删除教室时级联删除所有座位 |
| `UpdateRoomUseCase` | **改** — 禁止修改 layout |
| `ManageSeatsUseCase` | **删** |
| `DeleteSeatUseCase` | **删** |
| `UpdateSeatUseCase` | **改** — 入参从 `seatId(String)` 改为 `(roomId, seatId(int))` |
| `ListSeatsUseCase` | **改** — 返回 int 序号，去 seatNumber |
| `ListRoomsUseCase` | **改** — 返回 layout 信息，去 capacity |
| `ReserveUseCase` 等预约用例 | **改** — seatId 从 UUID 改为 (roomId, seatId) |

### 创建教室流程

```
管理员请求: { name, location, layout: "SMALL" }
         ↓
  系统创建 StudyRoom
         ↓
  根据 layout.seatCount (40)，批量生成 40 个 Seat:
  { roomId, id: 1, status: AVAILABLE }
  { roomId, id: 2, status: AVAILABLE }
  ...
  { roomId, id: 40, status: AVAILABLE }
         ↓
  教室 + 40 个座位一起持久化（事务性）
```

## 四、API / DTO 层变更

### CreateRoomRequest（修改）

```java
// capacity → layout
public record CreateRoomRequest(
    String name,
    String location,
    String layout      // "SMALL" / "MEDIUM" / "LARGE"
) {}
```

### 删除的 DTO

- `CreateSeatRequest` — 不再单独创建座位

### 修改的 DTO

- `UpdateSeatRequest` — seatId 改为 `(roomId, seatId(int))`
- `SeatResponse` — id 改为 int，去掉 seatNumber

### ReserveInput（预约请求）

```java
// 之前：传 seatId (UUID)
public record ReserveInput(String seatId, String timeSlotId, LocalDate date) {}

// 之后：传 roomId + seatId(序号)
public record ReserveInput(String roomId, int seatId, String timeSlotId, LocalDate date) {}
```

## 五、完整变更清单

| 层 | 文件 | 操作 |
|---|---|---|
| domain | 新增 `RoomLayout` 枚举 | 新增 |
| domain | `StudyRoom` — capacity → layout | 修改 |
| domain | `Seat` — id 改 int，去 seatNumber | 修改 |
| outbound | `SeatRepository` — 复合键查询 + deleteByRoomId | 修改 |
| outbound | `RoomRepository` | 不变 |
| usecase | `ManageRoomsUseCase` — 创建时自动生成座位 | 修改 |
| usecase | `DeleteRoomUseCase` — 级联删座位 | 修改 |
| usecase | `UpdateRoomUseCase` — 禁止改 layout | 修改 |
| usecase | `ManageSeatsUseCase` | **删除** |
| usecase | `DeleteSeatUseCase` | **删除** |
| usecase | `UpdateSeatUseCase` — 入参改 (roomId, seatId) | 修改 |
| usecase | `ListSeatsUseCase` — 返回 int 序号 | 修改 |
| usecase | `ListRoomsUseCase` — 返回 layout | 修改 |
| usecase | `ReserveUseCase` 等 — seatId 改为 roomId+seatId | 修改 |
| dto | `CreateRoomRequest` — capacity → layout | 修改 |
| dto | `CreateSeatRequest` | **删除** |
| dto | `UpdateSeatRequest` — 复合键 | 修改 |
| dto | `SeatResponse` — int id，去 seatNumber | 修改 |
| dto | `ReserveInput` — roomId + int seatId | 修改 |
| dto | `RoomResponse` / `RoomListResponse` — layout 替 capacity | 修改 |
| infra | `InMemorySeatRepo` — 适配新接口 | 修改 |
| infra | `InMemoryRoomRepo` | 修改 |
| infra | 所有 TestData — 适配新模型 | 修改 |
| test | 修改所有相关单元测试和集成测试 | 修改 |

## 六、约束与边界

- 教室创建后布局不可变更
- 座位不可单独新增或删除
- 座位序号 1..N 在教室创建时确定，不可修改
- 系统预设布局枚举值，管理员只能从中选择
