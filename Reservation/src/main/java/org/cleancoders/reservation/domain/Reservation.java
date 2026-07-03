package org.cleancoders.reservation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A seat reservation made by a student for a specific seat, date, and time slot.
 * <p>
 * The seat is identified by the composite key (roomId, seatId).
 */
public class Reservation {

    private String id;
    private final String userId;
    private final String roomId;
    private final int seatId;
    private final String timeSlotId;
    private final LocalDate date;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    public Reservation(String id, String userId, String roomId, int seatId, String timeSlotId, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.seatId = seatId;
        this.timeSlotId = timeSlotId;
        this.date = date;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = LocalDateTime.now();
    }

    // --- domain business logic ---

    public void cancel() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能取消，当前状态: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void checkIn() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "只有已预约状态才能签到，当前状态: " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
        this.checkInAt = LocalDateTime.now();
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "只有已签到状态才能退座，当前状态: " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
        this.checkOutAt = LocalDateTime.now();
    }

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

    public String roomId() {
        return roomId;
    }

    public int seatId() {
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
