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
        if (status != SeatStatus.OCCUPIED) {
            throw new IllegalStateException(
                    "只有使用中的座位才能释放，当前状态: " + status);
        }
        this.status = SeatStatus.AVAILABLE;
    }

    public void occupy() {
        if (status != SeatStatus.AVAILABLE && status != SeatStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有可用或已预约的座位才能签到，当前状态: " + status);
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
