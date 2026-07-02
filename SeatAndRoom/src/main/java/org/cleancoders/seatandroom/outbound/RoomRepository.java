package org.cleancoders.seatandroom.outbound;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for {@link StudyRoom} aggregates.
 */
public interface RoomRepository
{
    List<StudyRoom> findByStatus(RoomStatus status);

    Optional<StudyRoom> findById(String id);

    StudyRoom save(StudyRoom room);

    Optional<StudyRoom> findByName(String name);
}
