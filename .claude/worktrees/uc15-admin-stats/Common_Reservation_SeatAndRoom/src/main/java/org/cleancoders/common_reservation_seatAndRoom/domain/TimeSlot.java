package org.cleancoders.common_reservation_seatAndRoom.domain;

/**
 * A fixed time slot template for reservations.
 * Examples: 08:00-12:00, 13:00-17:00, 18:00-22:00.
 */
public record TimeSlot(
        String id,
        String startTime,
        String endTime,
        String label
) {
}
