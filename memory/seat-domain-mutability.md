---
name: seat-domain-mutability
description: Seat entity mutability — only id and status are mutable; roomId and seatNumber are final, behavior is status-transition methods only
metadata:
  type: project
---

In `Common_Reservation_SeatAndRoom/.../domain/Seat.java`, `id` and `status` are mutable, but **`roomId` and `seatNumber`
are `final`** (no setters). Domain behavior is status transitions only: `markMaintenance()` (AVAILABLE→MAINTENANCE),
`markAvailable()` (MAINTENANCE→AVAILABLE), `reserve()` (AVAILABLE→RESERVED), `release()`, `occupy()` (
RESERVED→OCCUPIED). Each throws IllegalStateException on illegal transitions.

**Why:** The domain deliberately models seats as immutable in identity (room/number) and mutable only in lifecycle
status. Renaming or moving a seat is not a supported operation — you delete and recreate.

**How to apply:** When implementing PUT /api/admin/seats/{id} (UC-07 update), "update" maps cleanly to **status
switching** (AVAILABLE↔MAINTENANCE), not to renaming. Don't propose making seatNumber/roomId mutable. A
reserved/occupied seat cannot be put into maintenance without releasing it first — the domain method enforces this and
throws, which the use case must present as a business error. Related: [[uc07-seat-crud-split-pattern]].