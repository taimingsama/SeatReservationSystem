package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

import java.util.UUID;

/**
 * UC-06: 管理员管理自习室（创建、更新、删除等）。
 * <p>
 * 当前实现 UC-06：创建自习室。根据所选布局自动生成 N 个座位。
 */
public class ManageRoomsUseCase extends AdminAuthUseCase<ManageRoomsUseCase.Request, ManageRoomsUseCase.Output>
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
        // Check name uniqueness
        var existing = roomRepo.findByName(req.name());
        if (existing.isPresent())
        {
            presenter.roomNameAlreadyExists(req.name());
            return null;
        }

        // Parse layout
        RoomLayout layout;
        try
        {
            layout = RoomLayout.valueOf(req.layout());
        }
        catch (IllegalArgumentException e)
        {
            presenter.invalidLayout(req.layout());
            return null;
        }

        String id = UUID.randomUUID().toString();
        StudyRoom room = new StudyRoom(id, req.name(), req.location(), layout, RoomStatus.OPEN);
        StudyRoom saved = roomRepo.save(room);

        // Auto-generate seats based on layout
        for (int i = 1; i <= layout.seatCount(); i++)
        {
            seatRepo.save(new Seat(i, id, SeatStatus.AVAILABLE));
        }

        presenter.success(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void success(StudyRoom room);

        void roomNameAlreadyExists(String name);

        void invalidLayout(String layout);
    }

    public record Request(String token, String name, String location, String layout)
            implements AuthUseCase.Request
    {
    }

    public record Output(String roomId)
    {
    }
}
