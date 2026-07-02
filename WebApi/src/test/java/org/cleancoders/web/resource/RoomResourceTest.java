package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.web.presenter.WebApiRoomPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomResourceTest
{

    private RoomResource resource;
    private WebApiRoomPresenter presenter;
    private boolean executeCalled;
    private ListRoomsUseCase.Output outputToReturn;

    @BeforeEach
    void setUp()
    {
        presenter = new WebApiRoomPresenter();
        executeCalled = false;
        outputToReturn = null;

        resource = new RoomResource();
        resource.presenter = presenter;
        resource.listRoomsUseCase = new ListRoomsUseCase()
        {
            @Override
            public Output execute(Request request)
            {
                executeCalled = true;
                return outputToReturn;
            }
        };
    }

    @Test
    void listShouldDelegateToUseCase()
    {
        outputToReturn = new ListRoomsUseCase.Output(List.of());

        resource.listRooms();

        assertTrue(executeCalled);
    }

    @Test
    void listShouldReturn200WithRooms()
    {
        StudyRoom open = new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN);
        outputToReturn = new ListRoomsUseCase.Output(List.of(open));
        presenter.presentRooms(List.of(open));

        Response response = resource.listRooms();

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void listShouldReturn200AndEmptyBodyWhenNoRooms()
    {
        outputToReturn = new ListRoomsUseCase.Output(List.of());
        presenter.presentRooms(List.of());

        Response response = resource.listRooms();

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }
}
