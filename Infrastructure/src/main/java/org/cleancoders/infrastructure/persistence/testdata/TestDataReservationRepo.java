package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.infrastructure.persistence.InMemoryReservationRepo;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@link InMemoryReservationRepo} pre-seeded with test reservations.
 * <p>
 * Populates reservations with various statuses (RESERVED, CHECKED_IN, CHECKED_OUT, CANCELLED)
 * across different users, seats, and time slots for comprehensive testing.
 */
public class TestDataReservationRepo extends InMemoryReservationRepo {

    public TestDataReservationRepo() {
        LocalDate today = LocalDate.now();

        // --- RESERVED reservations ---
        Reservation r1 = new Reservation("res-1", "user-zhangsan", "seat-1", "ts-2", today);
        save(r1);

        Reservation r2 = new Reservation("res-2", "user-lisi", "seat-3", "ts-2", today);
        save(r2);

        Reservation r3 = new Reservation("res-3", "user-wangwu", "seat-5", "ts-3", today);
        save(r3);

        // --- CHECKED_IN reservations ---
        Reservation r4 = new Reservation("res-4", "user-zhangsan", "seat-9", "ts-1", today);
        save(r4);
        r4.checkIn();

        Reservation r5 = new Reservation("res-5", "user-zhaoliu", "seat-10", "ts-1", today);
        save(r5);
        r5.checkIn();

        // --- CANCELLED reservations ---
        Reservation r6 = new Reservation("res-6", "user-lisi", "seat-2", "ts-3", today.minusDays(1));
        save(r6);
        r6.cancel();

        // --- CHECKED_OUT reservations ---
        Reservation r7 = new Reservation("res-7", "user-wangwu", "seat-13", "ts-1", today.minusDays(1));
        save(r7);
        r7.checkIn();
        r7.checkOut();

        // --- EXPIRED reservation ---
        Reservation r8 = new Reservation("res-8", "user-zhaoliu", "seat-7", "ts-2", today.minusDays(2));
        save(r8);
        r8.expire();
    }
}