package org.cleancoders.seatandroom.outbound;

/**
 * Narrow port for checking active reservations on a seat before deletion.
 */
public interface ActiveReservationChecker {

    /**
     * Returns {@code true} if the seat identified by (roomId, seatId)
     * has at least one active reservation (RESERVED or CHECKED_IN),
     * meaning it cannot be safely deleted.
     */
    boolean hasActiveForSeat(String roomId, int seatId);
}
