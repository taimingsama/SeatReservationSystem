package org.cleancoders.seatandroom.test.infrastructure;

import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.outbound.SeatRepository;

import java.util.List;
import java.util.Optional;

public class StubSeatRepo implements SeatRepository
{
    private final java.util.Map<String, Seat> seats = new java.util.LinkedHashMap<>();

    private static String key(String roomId, int seatId) {
        return roomId + ":" + seatId;
    }

    public void addSeat(Seat seat)
    {
        seats.put(key(seat.roomId(), seat.id()), seat);
    }

    @Override
    public Optional<Seat> findByRoomIdAndSeatId(String roomId, int seatId)
    {
        return Optional.ofNullable(seats.get(key(roomId, seatId)));
    }

    @Override
    public Seat save(Seat seat)
    {
        seats.put(key(seat.roomId(), seat.id()), seat);
        return seat;
    }

    @Override
    public List<Seat> findByRoomId(String roomId)
    {
        return seats.values().stream()
                .filter(s -> s.roomId().equals(roomId))
                .toList();
    }

    @Override
    public List<Seat> findAll()
    {
        return List.copyOf(seats.values());
    }

    @Override
    public void deleteByRoomId(String roomId)
    {
        seats.entrySet().removeIf(e -> e.getValue().roomId().equals(roomId));
    }
}
