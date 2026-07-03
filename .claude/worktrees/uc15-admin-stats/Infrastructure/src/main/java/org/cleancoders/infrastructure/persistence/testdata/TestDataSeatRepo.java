package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;

/**
 * {@link InMemorySeatRepo} pre-seeded with extra test seats.
 * <p>
 * Inherits the 12 base seats (seat-1 ~ seat-12) from {@link InMemorySeatRepo}
 * and adds additional seats for room-3, room-4, and room-5.
 */
public class TestDataSeatRepo extends InMemorySeatRepo {

    public TestDataSeatRepo() {
        // Inherits seat-1 ~ seat-8 (room-1) and seat-9 ~ seat-12 (room-2) from parent

        // room-3 seats (13-18)
        save(new Seat("seat-13", "room-3", "C-1", SeatStatus.AVAILABLE));
        save(new Seat("seat-14", "room-3", "C-2", SeatStatus.AVAILABLE));
        save(new Seat("seat-15", "room-3", "C-3", SeatStatus.RESERVED));
        save(new Seat("seat-16", "room-3", "C-4", SeatStatus.MAINTENANCE));
        save(new Seat("seat-17", "room-3", "C-5", SeatStatus.OCCUPIED));
        save(new Seat("seat-18", "room-3", "C-6", SeatStatus.AVAILABLE));

        // room-4 seats (19-22)
        save(new Seat("seat-19", "room-4", "D-1", SeatStatus.AVAILABLE));
        save(new Seat("seat-20", "room-4", "D-2", SeatStatus.AVAILABLE));
        save(new Seat("seat-21", "room-4", "D-3", SeatStatus.MAINTENANCE));
        save(new Seat("seat-22", "room-4", "D-4", SeatStatus.AVAILABLE));

        // room-5 seats (23-28)
        save(new Seat("seat-23", "room-5", "E-1", SeatStatus.AVAILABLE));
        save(new Seat("seat-24", "room-5", "E-2", SeatStatus.AVAILABLE));
        save(new Seat("seat-25", "room-5", "E-3", SeatStatus.OCCUPIED));
        save(new Seat("seat-26", "room-5", "E-4", SeatStatus.AVAILABLE));
        save(new Seat("seat-27", "room-5", "E-5", SeatStatus.RESERVED));
        save(new Seat("seat-28", "room-5", "E-6", SeatStatus.AVAILABLE));
    }
}