package org.cleancoders.seatandroom.outbound;

import org.cleancoders.seatandroom.domain.TimeSlot;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link TimeSlot} persistence.
 */
public interface TimeSlotRepository {

    Optional<TimeSlot> findById(String id);

    List<TimeSlot> findAll();
}
