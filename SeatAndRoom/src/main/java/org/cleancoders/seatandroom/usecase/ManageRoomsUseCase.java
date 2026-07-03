package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.UUID;

/**
 * UC-06/UC-07: 管理员管理自习室（创建、更新、删除等）。
 * <p>
 * 当前实现 UC-06：创建自习室。
 */
public class ManageRoomsUseCase extends AdminAuthUseCase<ManageRoomsUseCase.Request, ManageRoomsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    RoomRepository roomRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        // Check name uniqueness
        var existing = roomRepo.findByName(req.name());
        if (existing.isPresent())
        {
            presenter.roomNameAlreadyExists(req.name());
            return null;
        }

        String id = UUID.randomUUID().toString();
        StudyRoom room = new StudyRoom(id, req.name(), req.location(), req.capacity(), RoomStatus.OPEN);
        StudyRoom saved = roomRepo.save(room);
        presenter.success(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void success(StudyRoom room);

        void roomNameAlreadyExists(String name);
    }

    public record Request(String token, String name, String location, int capacity)
            implements AuthUseCase.Request
    {
    }

    public record Output(String roomId)
    {
    }
}