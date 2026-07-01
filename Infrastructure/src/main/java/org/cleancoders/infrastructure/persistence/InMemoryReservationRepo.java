package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemoryReservationRepo implements ReservationRepository {

    private final Map<String, Reservation> store = new ConcurrentHashMap<>();

    @Override
    public Reservation save(Reservation reservation) {
        if (reservation.id() == null) {
            reservation.setId(UUID.randomUUID().toString());
        }
        store.put(reservation.id(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
            String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
        return store.values().stream()
                .filter(r -> r.userId().equals(userId))
                .filter(r -> r.date().equals(date))
                .filter(r -> r.timeSlotId().equals(timeSlotId))
                .filter(r -> statuses.contains(r.status()))
                .findFirst();
    }

    @Override
    public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
            String seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
        return store.values().stream()
                .filter(r -> r.seatId().equals(seatId))
                .filter(r -> r.date().equals(date))
                .filter(r -> r.timeSlotId().equals(timeSlotId))
                .filter(r -> statuses.contains(r.status()))
                .findFirst();
    }
}
