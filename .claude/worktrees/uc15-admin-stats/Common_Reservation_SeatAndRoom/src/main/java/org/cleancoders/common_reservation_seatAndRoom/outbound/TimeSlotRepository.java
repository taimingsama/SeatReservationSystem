package org.cleancoders.common_reservation_seatAndRoom.outbound;

import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link TimeSlot} persistence.
 */
public interface TimeSlotRepository
{

    Optional<TimeSlot> findById(String id);

    List<TimeSlot> findAll();
}
