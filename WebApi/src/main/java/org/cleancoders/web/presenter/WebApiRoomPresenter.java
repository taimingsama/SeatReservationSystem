package org.cleancoders.web.presenter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.seatandroom.usecase.ListSeatsUseCase;
import org.cleancoders.web.dto.room.RoomListResponse;
import org.cleancoders.web.dto.room.RoomResponse;
import org.cleancoders.web.dto.seat.SeatListResponse;
import org.cleancoders.web.dto.seat.SeatResponse;

import java.util.List;
import java.util.Map;

/**
 * WebApi presenter for {@link ListRoomsUseCase} and {@link ListSeatsUseCase}.
 * Public use cases — no auth branches, so extends {@link WebApiPresenter}
 * directly rather than {@link WebApiPresenter}.
 */
@Singleton
public class WebApiRoomPresenter extends WebApiPresenter implements
        ListRoomsUseCase.Presenter,
        ListSeatsUseCase.Presenter
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
}