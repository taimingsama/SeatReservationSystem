package org.cleancoders.seatandroom.test.infrastructure;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.List;
import java.util.Optional;

public class StubRoomRepo implements RoomRepository
{
    private final java.util.Map<String, StudyRoom> rooms = new java.util.LinkedHashMap<>();
    public RoomStatus lastQueriedStatus;

    public void add(StudyRoom... toAdd)
    {
        for (StudyRoom r : toAdd)
        {
            rooms.put(r.id(), r);
        }
    }

    @Override
    public List<StudyRoom> findByStatus(RoomStatus status)
    {
        lastQueriedStatus = status;
        return rooms.values().stream()
                .filter(r -> r.status() == status)
                .toList();
    }

    @Override
    public Optional<StudyRoom> findById(String id)
    {
        return Optional.ofNullable(rooms.get(id));
    }

    @Override
    public Optional<StudyRoom> findByName(String name)
    {
        return rooms.values().stream()
                .filter(r -> r.name().equals(name))
                .findFirst();
    }

    @Override
    public StudyRoom save(StudyRoom room)
    {
        rooms.put(room.id(), room);
        return room;
    }
}
