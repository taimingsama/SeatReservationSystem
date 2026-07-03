package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;
import org.cleancoders.infrastructure.persistence.InMemoryTimeSlotRepo;

/**
 * {@link InMemoryTimeSlotRepo} pre-seeded with extra test time slots.
 * <p>
 * Inherits the 3 base time slots (ts-1 ~ ts-3) from {@link InMemoryTimeSlotRepo}
 * and adds a late-night slot for flexible testing.
 */
public class TestDataTimeSlotRepo extends InMemoryTimeSlotRepo {

    public TestDataTimeSlotRepo() {
        // Inherits ts-1 (08:00-12:00), ts-2 (13:00-17:00), ts-3 (18:00-22:00) from parent

        // Additional slot for edge-case testing
        store.put("ts-4", new TimeSlot("ts-4", "22:00", "23:59", "深夜 22:00-23:59"));
    }
}