package org.cleancoders.infrastructure.persistence.testdata;

import org.cleancoders.infrastructure.persistence.InMemoryRoomRepo;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;

/**
 * {@link InMemoryRoomRepo} pre-seeded with test study rooms.
 */
public class TestDataRoomRepo extends InMemoryRoomRepo {

    public TestDataRoomRepo() {
        save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        save(new StudyRoom("room-2", "自习室B", "图书馆二楼", 20, RoomStatus.OPEN));
        save(new StudyRoom("room-3", "自习室C", "教学楼三楼", 15, RoomStatus.MAINTENANCE));
        save(new StudyRoom("room-4", "自习室D", "图书馆三楼", 25, RoomStatus.CLOSED));
        save(new StudyRoom("room-5", "自习室E", "综合楼一楼", 40, RoomStatus.OPEN));
    }
}