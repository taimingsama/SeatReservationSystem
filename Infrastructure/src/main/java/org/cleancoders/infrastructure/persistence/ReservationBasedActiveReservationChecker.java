package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;

import java.util.Set;

@Singleton
public class ReservationBasedActiveReservationChecker implements ActiveReservationChecker {

    @Inject
    ReservationRepository reservationRepo;

    @Override
    public boolean hasActiveForSeat(String seatId) {
        return !reservationRepo.findBySeatIdAndStatusIn(seatId,
                Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN)).isEmpty();
    }
}
