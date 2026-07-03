package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

/**
 * UC-06: 删除自习室（管理员）。
 * <p>
 * 将自习室状态标记为 CLOSED（软删除），不真正移除数据。
 */
public class DeleteRoomUseCase extends AdminAuthUseCase<DeleteRoomUseCase.Request, DeleteRoomUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    RoomRepository roomRepo;

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

        StudyRoom closed = new StudyRoom(room.id(), room.name(), room.location(), room.capacity(), RoomStatus.CLOSED);
        roomRepo.save(closed);
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