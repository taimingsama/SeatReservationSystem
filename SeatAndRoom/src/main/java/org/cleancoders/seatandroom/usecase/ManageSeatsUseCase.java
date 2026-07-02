package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.UUID;

/**
 * UC-07: 管理员管理座位（创建、更新、删除、状态切换等）。
 * <p>
 * 当前实现 UC-07：创建座位。校验所属自习室存在，且同一自习室内座位编号唯一，
 * 新建座位状态默认为 {@link SeatStatus#AVAILABLE}。
 */
public class ManageSeatsUseCase extends AdminAuthUseCase<ManageSeatsUseCase.Request, ManageSeatsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    SeatRepository seatRepo;

    @Inject
    RoomRepository roomRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        // Check that the owning room exists
        var room = roomRepo.findById(req.roomId());
        if (room.isEmpty())
        {
            presenter.roomNotFound(req.roomId());
            return null;
        }

        // Check seat-number uniqueness within the room
        boolean duplicate = seatRepo.findByRoomId(req.roomId()).stream()
                .anyMatch(s -> s.seatNumber().equals(req.seatNumber()));
        if (duplicate)
        {
            presenter.seatNumberAlreadyExists(req.roomId(), req.seatNumber());
            return null;
        }

        String id = UUID.randomUUID().toString();
        Seat seat = new Seat(id, req.roomId(), req.seatNumber(), SeatStatus.AVAILABLE);
        Seat saved = seatRepo.save(seat);
        presenter.success(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void success(Seat seat);

        void roomNotFound(String roomId);

        void seatNumberAlreadyExists(String roomId, String seatNumber);
    }

    public record Request(String token, String roomId, String seatNumber)
            implements AuthUseCase.Request
    {
    }

    public record Output(String seatId)
    {
    }
}