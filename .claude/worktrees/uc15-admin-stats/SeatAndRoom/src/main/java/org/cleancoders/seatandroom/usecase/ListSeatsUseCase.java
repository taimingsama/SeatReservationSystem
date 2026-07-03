package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.List;
import java.util.Optional;

/**
 * UC-05: 获取某自习室所有座位及状态。
 * <p>
 * 公开用例（不继承 AuthUseCase，无认证要求）。
 */
public class ListSeatsUseCase
{

    @Inject
    RoomRepository roomRepo;

    @Inject
    SeatRepository seatRepo;

    @Inject
    Presenter presenter;

    public Output execute(Request request)
    {
        Optional<StudyRoom> room = roomRepo.findById(request.roomId());
        if (room.isEmpty())
        {
            presenter.roomNotFound(request.roomId());
            return new Output(List.of());
        }
        List<Seat> seats = seatRepo.findByRoomId(request.roomId());
        presenter.presentSeats(room.get(), seats);
        return new Output(seats);
    }

    public record Request(String roomId)
    {
    }

    public record Output(List<Seat> seats)
    {
    }

    public interface Presenter
    {
        void presentSeats(StudyRoom room, List<Seat> seats);

        void roomNotFound(String roomId);
    }
}