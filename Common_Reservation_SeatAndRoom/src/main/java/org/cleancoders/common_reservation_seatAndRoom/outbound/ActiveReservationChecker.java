package org.cleancoders.common_reservation_seatAndRoom.outbound;

/**
 * Narrow port for checking active reservations on a seat before deletion.
 * <p>
 * The definition of "active" (RESERVED / CHECKED_IN) is encapsulated in the
 * infrastructure implementation and is not visible to consumers in the
 * SeatAndRoom module.
 */
public interface ActiveReservationChecker {

    /**
     * Returns {@code true} if the seat has at least one active reservation
     * (RESERVED or CHECKED_IN), meaning it cannot be safely deleted.
     */
    boolean hasActiveForSeat(String seatId);
}
