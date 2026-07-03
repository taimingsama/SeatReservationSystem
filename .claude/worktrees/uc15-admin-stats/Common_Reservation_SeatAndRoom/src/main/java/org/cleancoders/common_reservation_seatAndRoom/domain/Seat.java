package org.cleancoders.common_reservation_seatAndRoom.domain;

/**
 * A seat within a study room.
 * <p>
 * Mutable class — status transitions are domain operations that validate
 * the current state before allowing the change.
 */
public class Seat {

    private String id;
    private final String roomId;
    private final String seatNumber;
    private SeatStatus status;

    public Seat(String id, String roomId, String seatNumber, SeatStatus status) {
        this.id = id;
        this.roomId = roomId;
        this.seatNumber = seatNumber;
        this.status = status;
    }

    // --- domain business logic ---

    /**
     * Marks the seat as reserved.
     *
     * @throws IllegalStateException if the seat is not {@link SeatStatus#AVAILABLE}
     */
    public void reserve() {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "只有可用状态的座位才能预约，当前状态: " + status);
        }
        this.status = SeatStatus.RESERVED;
    }

    /**
     * Releases the seat back to available.
     *
     * @throws IllegalStateException if the seat is not in a reservable/occupied state
     */
    public void release() {
        if (status != SeatStatus.RESERVED && status != SeatStatus.OCCUPIED) {
            throw new IllegalStateException(
                    "只有已预约或使用中的座位才能释放，当前状态: " + status);
        }
        this.status = SeatStatus.AVAILABLE;
    }

    /**
     * Marks the seat as occupied (after check-in).
     *
     * @throws IllegalStateException if the seat is not {@link SeatStatus#RESERVED}
     */
    public void occupy() {
        if (status != SeatStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约的座位才能签到，当前状态: " + status);
        }
        this.status = SeatStatus.OCCUPIED;
    }

    /**
     * Puts the seat under maintenance.
     *
     * @throws IllegalStateException if the seat is not {@link SeatStatus#AVAILABLE}
     */
    public void markMaintenance() {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "只有可用状态的座位才能设为维护，当前状态: " + status);
        }
        this.status = SeatStatus.MAINTENANCE;
    }

    /**
     * Restores the seat from maintenance to available.
     *
     * @throws IllegalStateException if the seat is not {@link SeatStatus#MAINTENANCE}
     */
    public void markAvailable() {
        if (status != SeatStatus.MAINTENANCE) {
            throw new IllegalStateException(
                    "只有维护中的座位才能恢复，当前状态: " + status);
        }
        this.status = SeatStatus.AVAILABLE;
    }

    // --- getters ---

    public String id() {
        return id;
    }

    public String roomId() {
        return roomId;
    }

    public String seatNumber() {
        return seatNumber;
    }

    public SeatStatus status() {
        return status;
    }

    // --- setters (for infrastructure use) ---

    public void setId(String id) {
        this.id = id;
    }
}
