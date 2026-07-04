package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;

import java.time.LocalDate;
import java.util.Set;

@Singleton
public class ReservationBasedActiveReservationChecker implements ActiveReservationChecker {

    @Inject
    ReservationRepository reservationRepo;

    @Override
    public boolean hasActiveForSeat(String roomId, int seatId) {
        return !reservationRepo.findBySeatIdAndStatusIn(roomId, seatId,
                Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN)).isEmpty();
    }

    @Override
    public boolean isReservedForTimeSlot(String roomId, int seatId, String timeSlotId, LocalDate date) {
        return reservationRepo.findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                roomId, seatId, date, timeSlotId,
                Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN)).isPresent();
    }

    @Override
    public boolean isCheckedInForTimeSlot(String roomId, int seatId, String timeSlotId, LocalDate date) {
        return reservationRepo.findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                roomId, seatId, date, timeSlotId,
                Set.of(ReservationStatus.CHECKED_IN)).isPresent();
    }
}
