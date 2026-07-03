package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.*;
import org.cleancoders.web.dto.room.RoomListResponse;
import org.cleancoders.web.dto.room.RoomResponse;
import org.cleancoders.web.dto.seat.SeatListResponse;
import org.cleancoders.web.dto.seat.SeatResponse;

import java.util.List;
import java.util.Map;

/**
 * WebApi presenter for {@link ListRoomsUseCase}, {@link ListSeatsUseCase},
 * {@link ManageRoomsUseCase}, {@link UpdateRoomUseCase}, {@link DeleteRoomUseCase}
 * and {@link ManageSeatsUseCase}.
 * <p>
 * Implements each use case's own {@code Presenter} (success / business-error
 * branches). Auth-related branches (401 invalid token, 404 user not found,
 * 403 forbidden) are handled by {@link WebApiAuthPresenter}, which is bound
 * to {@link org.cleancoders.common.usecase.AuthUseCase.Presenter} /
 * {@link org.cleancoders.common.usecase.AdminAuthUseCase.Presenter} and
 * injected into the base-class presenter fields.
 */
@Singleton
public class WebApiRoomPresenter extends WebApiPresenter implements
        ListRoomsUseCase.Presenter,
        ListSeatsUseCase.Presenter,
        ManageRoomsUseCase.Presenter,
        UpdateRoomUseCase.Presenter,
        DeleteRoomUseCase.Presenter,
        ManageSeatsUseCase.Presenter,
        UpdateSeatUseCase.Presenter
{

    @Override
    public void presentRooms(List<StudyRoom> rooms)
    {
        List<RoomResponse> dtos = rooms.stream()
                .map(r -> new RoomResponse(r.id(), r.name(), r.location(), r.capacity(), r.status()))
                .toList();
        responseContext.set(Response.ok(new RoomListResponse(dtos)).build());
    }

    @Override
    public void presentSeats(StudyRoom room, List<Seat> seats)
    {
        List<SeatResponse> dtos = seats.stream()
                .map(s -> new SeatResponse(s.id(), s.seatNumber(), s.status()))
                .toList();
        responseContext.set(Response.ok(new SeatListResponse(room.id(), room.name(), dtos)).build());
    }

    @Override
    public void roomNotFound(String roomId)
    {
        responseContext.set(Response.status(404).entity(Map.of(
                "error", "自习室不存在",
                "roomId", roomId
        )).build());
    }

    @Override
    public void success(StudyRoom room)
    {
        responseContext.set(Response.status(201).entity(
                new RoomResponse(room.id(), room.name(), room.location(), room.capacity(), room.status())
        ).build());
    }

    @Override
    public void updateSuccess(StudyRoom room)
    {
        responseContext.set(Response.ok(
                new RoomResponse(room.id(), room.name(), room.location(), room.capacity(), room.status())
        ).build());
    }

    @Override
    public void roomNameAlreadyExists(String name)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "自习室名称已存在",
                "name", name
        )).build());
    }

    @Override
    public void deleteSuccess(String roomId)
    {
        responseContext.set(Response.status(200).entity(Map.of(
                "message", "自习室已删除",
                "roomId", roomId
        )).build());
    }

    @Override
    public void roomAlreadyClosed(String roomId)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "自习室已处于关闭状态",
                "roomId", roomId
        )).build());
    }

    // --- ManageSeatsUseCase (UC-07 create seat) ---

    @Override
    public void success(Seat seat)
    {
        responseContext.set(Response.status(201).entity(
                new SeatResponse(seat.id(), seat.seatNumber(), seat.status())
        ).build());
    }

    @Override
    public void seatNumberAlreadyExists(String roomId, String seatNumber)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "座位编号已存在",
                "roomId", roomId,
                "seatNumber", seatNumber
        )).build());
    }

    // --- UpdateSeatUseCase (UC-07 update seat status) ---

    @Override
    public void updateSuccess(Seat seat)
    {
        responseContext.set(Response.ok(
                new SeatResponse(seat.id(), seat.seatNumber(), seat.status())
        ).build());
    }

    @Override
    public void seatNotFound(String seatId)
    {
        responseContext.set(Response.status(404).entity(Map.of(
                "error", "座位不存在",
                "seatId", seatId
        )).build());
    }

    @Override
    public void invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "非法状态转换",
                "seatId", seatId,
                "currentStatus", current.name(),
                "targetStatus", target.name()
        )).build());
    }

    @Override
    public void invalidStatus(String seatId, String status)
    {
        responseContext.set(Response.status(400).entity(Map.of(
                "error", "非法座位状态",
                "seatId", seatId,
                "status", status == null ? "null" : status
        )).build());
    }
}