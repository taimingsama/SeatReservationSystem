package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemorySeatRepo implements SeatRepository {

    private final Map<String, Seat> store = new ConcurrentHashMap<>();

    public InMemorySeatRepo() {
        // Pre-seed sample seats in a default study room
        String room1 = "room-1";
        for (int i = 1; i <= 8; i++) {
            String seatId = "seat-" + i;
            store.put(seatId, new Seat(seatId, room1, "A-" + i, SeatStatus.AVAILABLE));
        }
        String room2 = "room-2";
        for (int i = 9; i <= 12; i++) {
            String seatId = "seat-" + i;
            store.put(seatId, new Seat(seatId, room2, "B-" + (i - 8), SeatStatus.AVAILABLE));
        }
    }

    @Override
    public Optional<Seat> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Seat save(Seat seat) {
        if (seat.id() == null) {
            seat.setId(UUID.randomUUID().toString());
        }
        store.put(seat.id(), seat);
        return seat;
    }

    @Override
    public List<Seat> findByRoomId(String roomId) {
        return store.values().stream()
                .filter(s -> s.roomId().equals(roomId))
                .toList();
    }
}
