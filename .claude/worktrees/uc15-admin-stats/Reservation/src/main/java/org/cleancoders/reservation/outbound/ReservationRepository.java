package org.cleancoders.reservation.outbound;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for {@link Reservation} persistence.
 */
public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(String id);

    /**
     * Finds an active reservation for a user on a given date and time slot.
     * Used for duplicate detection (one person per time slot).
     */
    Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
            String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses);

    /**
     * Finds an active reservation for a seat on a given date and time slot.
     * Used for conflict detection (seat already booked).
     */
    Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
            String seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses);

    /**
     * Finds all reservations for a given user.
     */
    List<Reservation> findByUserId(String userId);

    List<Reservation> findAll();
}
