package org.cleancoders.seatandroom.outbound;

import java.time.LocalDate;

/**
 * Narrow port for checking active reservations on a seat.
 */
public interface ActiveReservationChecker {

    /**
     * Returns {@code true} if the seat identified by (roomId, seatId)
     * has at least one active reservation (RESERVED or CHECKED_IN),
     * meaning it cannot be safely deleted.
     */
    boolean hasActiveForSeat(String roomId, int seatId);

    /**
     * Returns {@code true} if the seat is actively reserved (RESERVED or CHECKED_IN)
     * for the specified time slot on the given date.
     */
    boolean isReservedForTimeSlot(String roomId, int seatId, String timeSlotId, LocalDate date);
}
