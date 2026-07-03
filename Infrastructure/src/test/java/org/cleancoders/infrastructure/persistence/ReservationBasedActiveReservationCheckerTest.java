package org.cleancoders.infrastructure.persistence;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationBasedActiveReservationCheckerTest
{
    private InMemoryReservationRepo repo;
    private ReservationBasedActiveReservationChecker checker;

    @BeforeEach
    void setUp()
    {
        repo = new InMemoryReservationRepo();
        checker = new ReservationBasedActiveReservationChecker();
        checker.reservationRepo = repo;
    }

    @Test
    void shouldReturnTrueWhenReservedExists()
    {
        repo.save(new Reservation("r1", "user-1", "room-1", 1, "ts-1", LocalDate.now()));
        assertTrue(checker.hasActiveForSeat("room-1", 1));
    }

    @Test
    void shouldReturnTrueWhenCheckedInExists()
    {
        Reservation r = new Reservation("r2", "user-1", "room-1", 1, "ts-1", LocalDate.now());
        r.checkIn();
        repo.save(r);
        assertTrue(checker.hasActiveForSeat("room-1", 1));
    }

    @Test
    void shouldReturnFalseWhenAllCancelledOrExpired()
    {
        Reservation cancelled = new Reservation("r3", "user-1", "room-1", 1, "ts-1", LocalDate.now());
        cancelled.cancel();
        repo.save(cancelled);

        Reservation expired = new Reservation("r4", "user-2", "room-1", 1, "ts-2", LocalDate.now());
        expired.expire();
        repo.save(expired);

        assertFalse(checker.hasActiveForSeat("room-1", 1));
    }

    @Test
    void shouldReturnFalseWhenNoReservations()
    {
        assertFalse(checker.hasActiveForSeat("room-1", 1));
    }
}
