package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemorySeatRepo implements SeatRepository {

    private final Map<String, Seat> store = new ConcurrentHashMap<>();

    private static String key(String roomId, int seatId) {
        return roomId + ":" + seatId;
    }

    public InMemorySeatRepo() {
        // Pre-seed: 8 seats in room-1
        String room1 = "room-1";
        for (int i = 1; i <= 8; i++) {
            store.put(key(room1, i), new Seat(i, room1, SeatStatus.AVAILABLE));
        }
        // 4 seats in room-2
        String room2 = "room-2";
        for (int i = 1; i <= 4; i++) {
            store.put(key(room2, i), new Seat(i, room2, SeatStatus.AVAILABLE));
        }
    }

    @Override
    public Optional<Seat> findByRoomIdAndSeatId(String roomId, int seatId) {
        return Optional.ofNullable(store.get(key(roomId, seatId)));
    }

    @Override
    public Seat save(Seat seat) {
        store.put(key(seat.roomId(), seat.id()), seat);
        return seat;
    }

    @Override
    public List<Seat> findByRoomId(String roomId) {
        return store.values().stream()
                .filter(s -> s.roomId().equals(roomId))
                .toList();
    }

    @Override
    public List<Seat> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void deleteByRoomId(String roomId) {
        store.entrySet().removeIf(e -> e.getValue().roomId().equals(roomId));
    }
}
