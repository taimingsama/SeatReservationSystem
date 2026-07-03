package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.util.List;

/**
 * UC-04: 获取所有 OPEN 状态的自习室。
 * <p>
 * 公开用例（不继承 AuthUseCase，无认证要求）。
 */
public class ListRoomsUseCase
{

    @Inject
    RoomRepository roomRepo;

    @Inject
    Presenter presenter;

    public Output execute(Request request)
    {
        List<StudyRoom> rooms = roomRepo.findByStatus(RoomStatus.OPEN);
        presenter.presentRooms(rooms);
        return new Output(rooms);
    }

    public record Request()
    {
    }

    public record Output(List<StudyRoom> rooms)
    {
    }

    public interface Presenter
    {
        void presentRooms(List<StudyRoom> rooms);
    }
}
