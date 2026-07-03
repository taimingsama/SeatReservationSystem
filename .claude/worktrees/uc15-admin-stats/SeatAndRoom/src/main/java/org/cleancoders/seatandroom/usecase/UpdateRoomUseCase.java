package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

/**
 * UC-06: 更新自习室信息（管理员）。
 */
public class UpdateRoomUseCase extends AdminAuthUseCase<UpdateRoomUseCase.Request, UpdateRoomUseCase.Output>
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

        // Check name uniqueness (exclude self)
        var nameConflict = roomRepo.findByName(req.name());
        if (nameConflict.isPresent() && !nameConflict.get().id().equals(req.roomId()))
        {
            presenter.roomNameAlreadyExists(req.name());
            return null;
        }

        StudyRoom old = existing.get();
        StudyRoom updated = new StudyRoom(old.id(), req.name(), req.location(), req.capacity(), old.status());
        StudyRoom saved = roomRepo.save(updated);
        presenter.updateSuccess(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void updateSuccess(StudyRoom room);

        void roomNotFound(String roomId);

        void roomNameAlreadyExists(String name);
    }

    public record Request(String token, String roomId, String name, String location, int capacity)
            implements AuthUseCase.Request
    {
    }

    public record Output(String roomId)
    {
    }
}