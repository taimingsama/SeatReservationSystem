package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;

/**
 * {@link InMemorySeatRepo} pre-seeded with extra test seats.
 * Inherits the base seats (room-1: 8 seats, room-2: 4 seats) from parent
 * and adds additional seats for room-3, room-4, and room-5.
 */
public class TestDataSeatRepo extends InMemorySeatRepo {

    public TestDataSeatRepo() {
        // Inherits room-1 (8 seats) and room-2 (4 seats) from parent

        // room-3: 6 seats
        save(new Seat(1, "room-3", SeatStatus.AVAILABLE));
        save(new Seat(2, "room-3", SeatStatus.AVAILABLE));
        save(new Seat(3, "room-3", SeatStatus.RESERVED));
        save(new Seat(4, "room-3", SeatStatus.MAINTENANCE));
        save(new Seat(5, "room-3", SeatStatus.OCCUPIED));
        save(new Seat(6, "room-3", SeatStatus.AVAILABLE));

        // room-4: 4 seats
        save(new Seat(1, "room-4", SeatStatus.AVAILABLE));
        save(new Seat(2, "room-4", SeatStatus.AVAILABLE));
        save(new Seat(3, "room-4", SeatStatus.MAINTENANCE));
        save(new Seat(4, "room-4", SeatStatus.AVAILABLE));

        // room-5: 6 seats
        save(new Seat(1, "room-5", SeatStatus.AVAILABLE));
        save(new Seat(2, "room-5", SeatStatus.AVAILABLE));
        save(new Seat(3, "room-5", SeatStatus.OCCUPIED));
        save(new Seat(4, "room-5", SeatStatus.AVAILABLE));
        save(new Seat(5, "room-5", SeatStatus.RESERVED));
        save(new Seat(6, "room-5", SeatStatus.AVAILABLE));
    }
}
