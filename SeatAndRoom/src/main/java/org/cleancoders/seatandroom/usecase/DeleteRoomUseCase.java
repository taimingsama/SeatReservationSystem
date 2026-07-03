package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

/**
 * UC-06: 删除自习室（管理员）。
 * <p>
 * 将自习室状态标记为 CLOSED（软删除），同时级联删除所有座位。
 */
public class DeleteRoomUseCase extends AdminAuthUseCase<DeleteRoomUseCase.Request, DeleteRoomUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    RoomRepository roomRepo;

    @Inject
    SeatRepository seatRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = roomRepo.findById(req.roomId());
        if (existing.isEmpty())
        {
            presenter.roomNotFound(req.roomId());
            return null;
        }

        StudyRoom room = existing.get();
        if (room.status() == RoomStatus.CLOSED)
        {
            presenter.roomAlreadyClosed(req.roomId());
            return null;
        }

        StudyRoom closed = new StudyRoom(room.id(), room.name(), room.location(), room.layout(), RoomStatus.CLOSED);
        roomRepo.save(closed);

        // Cascade delete seats
        seatRepo.deleteByRoomId(req.roomId());

        presenter.deleteSuccess(req.roomId());
        return new Output(req.roomId());
    }

    public interface Presenter
    {
        void deleteSuccess(String roomId);

        void roomNotFound(String roomId);

        void roomAlreadyClosed(String roomId);
    }

    public record Request(String token, String roomId)
            implements AuthUseCase.Request
    {
    }

    public record Output(String roomId)
    {
    }
}
