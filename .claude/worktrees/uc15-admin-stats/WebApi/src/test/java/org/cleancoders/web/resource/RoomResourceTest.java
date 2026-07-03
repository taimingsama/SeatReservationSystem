package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Response;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.seatandroom.usecase.ListSeatsUseCase;
import org.cleancoders.web.presenter.ResponseContext;
import org.cleancoders.web.presenter.WebApiRoomPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoomResourceTest
{

    private RoomResource resource;
    private WebApiRoomPresenter presenter;
    private ResponseContext ctx;
    private boolean listRoomsExecuteCalled;
    private ListRoomsUseCase.Output listRoomsOutput;
    private boolean listSeatsExecuteCalled;
    private ListSeatsUseCase.Request lastListSeatsRequest;

    @BeforeEach
    void setUp()
    {
        ctx = new ResponseContext();
        presenter = new WebApiRoomPresenter();
        presenter.responseContext = ctx;
        listRoomsExecuteCalled = false;
        listRoomsOutput = null;
        listSeatsExecuteCalled = false;
        lastListSeatsRequest = null;

        resource = new RoomResource();
        resource.responseContext = ctx;
        resource.listRoomsUseCase = new ListRoomsUseCase()
        {
            @Override
            public Output execute(Request request)
            {
                listRoomsExecuteCalled = true;
                return listRoomsOutput;
            }
        };
        resource.listSeatsUseCase = new ListSeatsUseCase()
        {
            @Override
            public Output execute(Request request)
            {
                listSeatsExecuteCalled = true;
                lastListSeatsRequest = request;
                return new Output(List.of());
            }
        };
    }

    // --- listRooms tests ---

    @Test
    void listShouldDelegateToUseCase()
    {
        listRoomsOutput = new ListRoomsUseCase.Output(List.of());

        resource.listRooms();

        assertTrue(listRoomsExecuteCalled);
    }

    @Test
    void listShouldReturn200WithRooms()
    {
        StudyRoom open = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);
        listRoomsOutput = new ListRoomsUseCase.Output(List.of(open));
        presenter.presentRooms(List.of(open));

        Response response = resource.listRooms();

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void listShouldReturn200AndEmptyBodyWhenNoRooms()
    {
        listRoomsOutput = new ListRoomsUseCase.Output(List.of());
        presenter.presentRooms(List.of());

        Response response = resource.listRooms();

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    // --- listSeats tests ---

    @Test
    void listSeatsShouldDelegateToUseCaseWithRoomId()
    {
        resource.listSeats("room-1");

        assertTrue(listSeatsExecuteCalled);
        assertEquals("room-1", lastListSeatsRequest.roomId());
    }

    @Test
    void listSeatsShouldReturn200WithSeats()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN);
        List<Seat> seats = List.of(
                new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE),
                new Seat("seat-2", "room-1", "A-2", SeatStatus.RESERVED)
        );
        presenter.presentSeats(room, seats);

        Response response = resource.listSeats("room-1");

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void listSeatsShouldReturn404WhenRoomNotFound()
    {
        presenter.roomNotFound("nonexistent");

        Response response = resource.listSeats("nonexistent");

        assertEquals(404, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertEquals("自习室不存在", body.get("error"));
        assertEquals("nonexistent", body.get("roomId"));
    }
}
