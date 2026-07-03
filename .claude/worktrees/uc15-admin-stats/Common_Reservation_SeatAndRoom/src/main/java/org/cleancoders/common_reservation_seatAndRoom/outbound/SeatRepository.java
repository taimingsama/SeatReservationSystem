package org.cleancoders.common_reservation_seatAndRoom.outbound;

import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Seat} persistence.
 */
public interface SeatRepository {

    Optional<Seat> findById(String id);

    Seat save(Seat seat);

    List<Seat> findByRoomId(String roomId);
}
