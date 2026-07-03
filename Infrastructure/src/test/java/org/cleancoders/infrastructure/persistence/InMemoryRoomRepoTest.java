package org.cleancoders.infrastructure.persistence;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRoomRepoTest
{

    private InMemoryRoomRepo repo;

    @BeforeEach
    void setUp()
    {
        repo = new InMemoryRoomRepo();
    }

    @Test
    void saveShouldStoreAndReturnRoom()
    {
        StudyRoom room = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);

        StudyRoom saved = repo.save(room);

        assertSame(room, saved);
        assertEquals(Optional.of(room), repo.findById("r1"));
    }

    @Test
    void findByIdShouldReturnEmptyWhenMissing()
    {
        assertTrue(repo.findById("nope").isEmpty());
    }

    @Test
    void findByStatusShouldFilterByStatus()
    {
        StudyRoom open = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);
        StudyRoom closed = new StudyRoom("r2", "B", "L2", 10, RoomStatus.CLOSED);
        StudyRoom open2 = new StudyRoom("r3", "C", "L3", 10, RoomStatus.OPEN);
        repo.save(open);
        repo.save(closed);
        repo.save(open2);

        var result = repo.findByStatus(RoomStatus.OPEN);

        assertEquals(2, result.size());
        assertTrue(result.contains(open));
        assertTrue(result.contains(open2));
        assertFalse(result.contains(closed));
    }

    @Test
    void findByStatusShouldReturnEmptyWhenNoMatch()
    {
        repo.save(new StudyRoom("r1", "A", "L1", 10, RoomStatus.CLOSED));

        assertTrue(repo.findByStatus(RoomStatus.OPEN).isEmpty());
    }
}
