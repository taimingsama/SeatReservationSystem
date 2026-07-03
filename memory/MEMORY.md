# Memory Index

- [Seat domain mutability](seat-domain-mutability.md) — Seat has final roomId/seatNumber; only status transitions are
  domain ops
- [UC-07 seat CRUD split pattern](uc07-seat-crud-split-pattern.md) — one use-case class per seat CRUD op (
  ManageSeatsUseCase=create only; PUT needs new UpdateSeatUseCase)