# 按时间段区分座位状态 — 设计文档

**日期**: 2026-07-04  
**状态**: 待评审

---

## 概述

`GET /rooms/{id}/seats?timeSlotId=ts-1&date=2026-07-04` 目前只区分 AVAILABLE 和 RESERVED。需要增强为根据时段类型（当前/未来/过去）返回不同粒度的状态。

---

## 时段分类

给定查询参数 `timeSlotId` + `date`，计算 `slotStart` 和 `slotEnd`：

```
slotStart = date + timeSlot.startTime
slotEnd   = date + timeSlot.endTime
now       = 当前时间
```

| 条件 | 分类 | 行为 |
|------|------|------|
| `slotEnd < now` | 过去时段 | 返回 400 |
| `slotStart ≤ now < slotEnd` | 当前时段 | 4 状态 |
| `now < slotStart` | 未来时段 | 3 状态 |

---

## 有效状态计算

```
Seat.status (静态)    时段分类      预约状态          最终状态
──────────────────────────────────────────────────────────
MAINTENANCE          任何          —               MAINTENANCE
REMOVED              任何          —               REMOVED（保持静态状态）

AVAILABLE            过去          —               400 (不进入计算)
AVAILABLE            当前          CHECKED_IN      OCCUPIED
AVAILABLE            当前          RESERVED        RESERVED
AVAILABLE            当前          无预约           AVAILABLE
AVAILABLE            未来          RESERVED/CHECKED_IN  RESERVED
AVAILABLE            未来          无预约           AVAILABLE
```

关键规则：
- **OCCUPIED 只出现在当前时段**（没人能在未来时段"使用中"）
- **MAINTENANCE 跨时段不变**
- **过去时段不允许查询**

---

## 接口变更

### ActiveReservationChecker

新增方法（替代纯 boolean 查询）：

```java
/**
 * 返回座位在指定时段的活跃预约状态。
 * @return Optional.empty() 表示无活跃预约；
 *         RESERVED 表示已预约未签到；
 *         CHECKED_IN 表示已签到使用中
 */
Optional<ReservationStatus> getReservationStatus(
    String roomId, int seatId, String timeSlotId, LocalDate date);
```

### ListSeatsUseCase

`computeEffectiveStatus` 重写，增加：
- 时段分类判断（过去 → 调 presenter 错误方法）
- 预约状态 → OCCUPIED / RESERVED 映射
- 未来时段 CHECKED_IN → RESERVED（不可能出现但防御性处理）

### WebApiRoomPresenter

新增：

```java
void pastTimeSlot(String timeSlotId, LocalDate date);
// → Response 400 "不能查询过去时段"
```

### RoomResource

不变。`timeSlotId` 和 `date` QueryParam 已存在。

---

## 测试要点

| 场景 | 预期 |
|------|------|
| 当前时段 + 无预约 | AVAILABLE |
| 当前时段 + RESERVED | RESERVED |
| 当前时段 + CHECKED_IN | OCCUPIED |
| 当前时段 + MAINTENANCE | MAINTENANCE |
| 未来时段 + 无预约 | AVAILABLE |
| 未来时段 + RESERVED | RESERVED |
| 未来时段 + CHECKED_IN | RESERVED（防御） |
| 未来时段 + MAINTENANCE | MAINTENANCE |
| 过去时段 | 400 |

---

## 影响范围

| 模块 | 文件 | 改动 |
|------|------|------|
| SeatAndRoom | `outbound/ActiveReservationChecker.java` | 新增 `getReservationStatus` |
| Infrastructure | `ReservationBasedActiveReservationChecker.java` | 实现新方法 |
| SeatAndRoom | `usecase/ListSeatsUseCase.java` | 重写 `computeEffectiveStatus` |
| WebApi | `presenter/WebApiRoomPresenter.java` | 新增 `pastTimeSlot` |
| SeatAndRoom | `usecase/ListSeatsUseCaseTest.java` | 新增测试 |
| WebApi | `RoomResourceIntegrationTest.java` | 新增集成测试 |
