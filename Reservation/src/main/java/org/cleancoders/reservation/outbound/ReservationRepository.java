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

    Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
            String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses);

    Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
            String roomId, int seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses);

    List<Reservation> findByUserId(String userId);

    List<Reservation> findAll();

    List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> statuses);
}
