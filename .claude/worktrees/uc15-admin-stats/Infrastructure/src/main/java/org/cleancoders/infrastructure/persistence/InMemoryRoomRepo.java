package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link RoomRepository}.
 */
@Singleton
public class InMemoryRoomRepo implements RoomRepository
{

    private final ConcurrentHashMap<String, StudyRoom> rooms = new ConcurrentHashMap<>();

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
    public StudyRoom save(StudyRoom room)
    {
        rooms.put(room.id(), room);
        return room;
    }

    @Override
    public Optional<StudyRoom> findByName(String name)
    {
        return rooms.values().stream()
                .filter(r -> r.name().equals(name))
                .findFirst();
    }
}
