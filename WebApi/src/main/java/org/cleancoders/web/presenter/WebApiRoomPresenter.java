package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.usecase.*;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.room.*;
import org.cleancoders.web.dto.seat.*;

import java.time.LocalDate;
import java.util.List;

/**
 * WebApi presenter for room and seat use cases.
 */
@Singleton
public class WebApiRoomPresenter extends WebApiPresenter implements
        ListRoomsUseCase.Presenter,
        ListSeatsUseCase.Presenter,
        ListTimeSlotsUseCase.Presenter,
        ManageRoomsUseCase.Presenter,
        UpdateRoomUseCase.Presenter,
        DeleteRoomUseCase.Presenter,
        UpdateSeatUseCase.Presenter
{

    @Override
    public void presentRooms(List<StudyRoom> rooms)
    {
        List<RoomResponse> dtos = rooms.stream()
                .map(r -> new RoomResponse(r.id(), r.name(), r.location(),
                        r.layout().name(), r.layout().seatCount(), r.status()))
                .toList();
        responseContext.set(Response.ok(new RoomListResponse(dtos)).build());
    }

    @Override
    public void presentTimeSlots(List<TimeSlot> slots)
    {
        List<TimeSlotResponse> dtos = slots.stream()
                .map(s -> new TimeSlotResponse(s.id(), s.startTime(), s.endTime(), s.label()))
                .toList();
        responseContext.set(Response.ok(new TimeSlotListResponse(dtos)).build());
    }

    @Override
    public void presentSeats(StudyRoom room, List<Seat> seats)
    {
        List<SeatResponse> dtos = seats.stream()
                .map(s -> new SeatResponse(s.id(), s.roomId(), s.status()))
                .toList();
        responseContext.set(Response.ok(new SeatListResponse(room.id(), room.name(), dtos)).build());
    }

    @Override
    public void roomNotFound(String roomId)
    {
        responseContext.set(Response.status(404).entity(
                new RoomNotFoundResponse("自习室不存在", roomId)
        ).build());
    }

    @Override
    public void pastTimeSlot(String timeSlotId, LocalDate date)
    {
        responseContext.set(Response.status(400).entity(
                new ErrorResponse("不能查询过去时段")).build());
    }

    @Override
    public void success(StudyRoom room)
    {
        responseContext.set(Response.status(201).entity(
                new RoomResponse(room.id(), room.name(), room.location(),
                        room.layout().name(), room.layout().seatCount(), room.status())
        ).build());
    }

    @Override
    public void updateSuccess(StudyRoom room)
    {
        responseContext.set(Response.ok(
                new RoomResponse(room.id(), room.name(), room.location(),
                        room.layout().name(), room.layout().seatCount(), room.status())
        ).build());
    }

    @Override
    public void roomNameAlreadyExists(String name)
    {
        responseContext.set(Response.status(409).entity(
                new RoomNameConflictResponse("自习室名称已存在", name)
        ).build());
    }

    @Override
    public void invalidLayout(String layout)
    {
        responseContext.set(Response.status(400).entity(
                new InvalidLayoutResponse("无效的布局类型", layout,
                        new String[]{"SMALL", "MEDIUM", "LARGE"})
        ).build());
    }

    @Override
    public void deleteSuccess(String roomId)
    {
        responseContext.set(Response.status(200).entity(
                new RoomDeletedResponse("自习室已删除", roomId)
        ).build());
    }

    @Override
    public void roomAlreadyClosed(String roomId)
    {
        responseContext.set(Response.status(409).entity(
                new RoomAlreadyClosedResponse("自习室已处于关闭状态", roomId)
        ).build());
    }

    // --- UpdateSeatUseCase ---

    @Override
    public void updateSuccess(Seat seat)
    {
        responseContext.set(Response.ok(
                new SeatResponse(seat.id(), seat.roomId(), seat.status())
        ).build());
    }

    @Override
    public void seatNotFound(String roomId, int seatId)
    {
        responseContext.set(Response.status(404).entity(
                new SeatNotFoundInRoomResponse("座位不存在", roomId, seatId)
        ).build());
    }

    @Override
    public void invalidStatusTransition(String roomId, int seatId, SeatStatus current, SeatStatus target)
    {
        responseContext.set(Response.status(409).entity(
                new InvalidStatusTransitionResponse("非法状态转换", roomId, seatId,
                        current.name(), target.name())
        ).build());
    }

    @Override
    public void invalidStatus(String roomId, int seatId, String status)
    {
        responseContext.set(Response.status(400).entity(
                new InvalidSeatStatusResponse("非法座位状态", roomId, seatId,
                        status == null ? "null" : status)
        ).build());
    }
}
