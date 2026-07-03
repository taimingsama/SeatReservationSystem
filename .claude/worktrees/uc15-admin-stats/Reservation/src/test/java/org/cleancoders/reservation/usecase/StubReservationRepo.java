package org.cleancoders.reservation.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class StubReservationRepo implements ReservationRepository
{
    private final java.util.Map<String, Reservation> reservations = new java.util.HashMap<>();

    void addReservation(Reservation r)
    {
        reservations.put(r.id(), r);
    }

    @Override
    public Reservation save(Reservation reservation)
    {
        if (reservation.id() == null)
            reservation.setId("generated-" + reservations.size());

        reservations.put(reservation.id(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(String id)
    {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
            String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses)
    {
        return reservations.values().stream()
                .filter(r -> r.userId().equals(userId))
                .filter(r -> r.date().equals(date))
                .filter(r -> r.timeSlotId().equals(timeSlotId))
                .filter(r -> statuses.contains(r.status()))
                .findFirst();
    }

    @Override
    public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
            String seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses)
    {
        return reservations.values().stream()
                .filter(r -> r.roomId().equals(roomId) && r.seatId() == seatId)
                .filter(r -> r.date().equals(date))
                .filter(r -> r.timeSlotId().equals(timeSlotId))
                .filter(r -> statuses.contains(r.status()))
                .findFirst();
    }

    @Override
    public List<Reservation> findByUserId(String userId)
    {
        return reservations.values().stream()
                .filter(r -> r.userId().equals(userId))
                .toList();
    }

    @Override
    public List<Reservation> findAll()
    {
        return List.copyOf(reservations.values());
    }
}
