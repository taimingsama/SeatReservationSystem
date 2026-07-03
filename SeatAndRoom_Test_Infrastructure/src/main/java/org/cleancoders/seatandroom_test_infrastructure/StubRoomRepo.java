package org.cleancoders.seatandroom_test_infrastructure;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StubRoomRepo implements RoomRepository
{
    private final Map<String, StudyRoom> rooms = new LinkedHashMap<>();

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
    public List<StudyRoom> findAll()
    {
        return rooms.values().stream().toList();
    }

    @Override
    public StudyRoom save(StudyRoom room)
    {
        rooms.put(room.id(), room);
        return room;
    }
}
