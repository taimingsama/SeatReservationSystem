package org.cleancoders.reservation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A seat reservation made by a student for a specific seat, date, and time slot.
 * <p>
 * Mutable class — status transitions are domain operations that validate
 * the current state before allowing the change.
 */
public class Reservation {

    private String id;
    private final String userId;
    private final String seatId;
    private final String timeSlotId;
    private final LocalDate date;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    /**
     * Creates a new reservation in {@link ReservationStatus#RESERVED} status.
     */
    public Reservation(String id, String userId, String seatId, String timeSlotId, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.seatId = seatId;
        this.timeSlotId = timeSlotId;
        this.date = date;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = LocalDateTime.now();
    }

    // --- domain business logic ---

    /**
     * Cancels this reservation.
     *
     * @throws IllegalStateException if not in {@link ReservationStatus#RESERVED} status
     */
    public void cancel() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能取消，当前状态: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * Checks in for this reservation.
     *
     * @throws IllegalStateException if not in {@link ReservationStatus#RESERVED} status
     */
    public void checkIn() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能签到，当前状态: " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
        this.checkInAt = LocalDateTime.now();
    }

    /**
     * Checks out from this reservation.
     *
     * @throws IllegalStateException if not in {@link ReservationStatus#CHECKED_IN} status
     */
    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "只有已签到状态才能退座，当前状态: " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
        this.checkOutAt = LocalDateTime.now();
    }

    /**
     * Marks this reservation as expired (no-show).
     *
     * @throws IllegalStateException if not in {@link ReservationStatus#RESERVED} status
     */
    public void expire() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能标记为过期，当前状态: " + status);
        }
        this.status = ReservationStatus.EXPIRED;
    }

    // --- getters ---

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String seatId() {
        return seatId;
    }

    public String timeSlotId() {
        return timeSlotId;
    }

    public LocalDate date() {
        return date;
    }

    public ReservationStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime checkInAt() {
        return checkInAt;
    }

    public LocalDateTime checkOutAt() {
        return checkOutAt;
    }

    // --- setters (for infrastructure use) ---

    public void setId(String id) {
        this.id = id;
    }
}
