package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.infrastructure.persistence.InMemoryRoomRepo;
import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.seatandroom.usecase.ListSeatsUseCase;
import org.cleancoders.web.binder.ReservationBinder;
import org.cleancoders.web.binder.SeatAndRoomBinder;
import org.cleancoders.web.binder.WebAppBinder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoomResourceIntegrationTest extends JerseyTest
{

    private InMemoryRoomRepo roomRepo;
    private InMemorySeatRepo seatRepo;

    @Override
    protected Application configure()
    {
        roomRepo = new InMemoryRoomRepo();
        seatRepo = new InMemorySeatRepo();

        ResourceConfig config = new ResourceConfig();
        config.register(RoomResource.class);
        config.register(WebAppBinder.class);
        config.register(ReservationBinder.class);
        config.register(SeatAndRoomBinder.class);
        config.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(roomRepo).to(RoomRepository.class);
                bind(seatRepo).to(SeatRepository.class);
                bind(ListRoomsUseCase.class).to(ListRoomsUseCase.class);
                bind(ListSeatsUseCase.class).to(ListSeatsUseCase.class);
            }
        });
        return config;
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    // --- listRooms tests ---

    @Test
    void shouldReturn200WithOnlyOpenRooms()
    {
        roomRepo.save(new StudyRoom("r1", "A", "L1", 10, RoomStatus.OPEN));
        roomRepo.save(new StudyRoom("r2", "B", "L2", 10, RoomStatus.CLOSED));
        roomRepo.save(new StudyRoom("r3", "C", "L3", 10, RoomStatus.OPEN));
        roomRepo.save(new StudyRoom("r4", "D", "L4", 10, RoomStatus.MAINTENANCE));

        Response response = target("/rooms").request(MediaType.APPLICATION_JSON).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) body.get("rooms");
        assertEquals(2, rooms.size());
        List<String> ids = rooms.stream().map(m -> (String) m.get("id")).toList();
        assertTrue(ids.contains("r1"));
        assertTrue(ids.contains("r3"));
        assertFalse(ids.contains("r2"));
        assertFalse(ids.contains("r4"));
    }

    @Test
    void shouldReturn200WithEmptyArrayWhenNoOpenRooms()
    {
        roomRepo.save(new StudyRoom("r1", "A", "L1", 10, RoomStatus.CLOSED));

        Response response = target("/rooms").request(MediaType.APPLICATION_JSON).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        @SuppressWarnings("unchecked")
        List<?> rooms = (List<?>) body.get("rooms");
        assertTrue(rooms.isEmpty());
    }

    // --- listSeats tests ---

    @Test
    void shouldReturn200WithSeatsForExistingRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/rooms/room-1/seats").request(MediaType.APPLICATION_JSON).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("room-1", body.get("roomId"));
        assertEquals("自习室A", body.get("roomName"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seats = (List<Map<String, Object>>) body.get("seats");
        assertEquals(8, seats.size());
        // Verify seat numbers belong to room-1
        List<String> seatNumbers = seats.stream().map(s -> (String) s.get("seatNumber")).toList();
        assertTrue(seatNumbers.contains("A-1"));
        assertTrue(seatNumbers.contains("A-8"));
    }

    @Test
    void shouldReturn404WhenRoomNotFound()
    {
        Response response = target("/rooms/nonexistent/seats").request(MediaType.APPLICATION_JSON).get();

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室不存在", body.get("error"));
        assertEquals("nonexistent", body.get("roomId"));
    }

    @Test
    void shouldReturn200WithSeatsOfAllStatuses()
    {
        roomRepo.save(new StudyRoom("room-3", "自习室C", "教学楼三楼", 15, RoomStatus.OPEN));
        seatRepo.save(new Seat("s1", "room-3", "C-1", SeatStatus.AVAILABLE));
        seatRepo.save(new Seat("s2", "room-3", "C-2", SeatStatus.RESERVED));
        seatRepo.save(new Seat("s3", "room-3", "C-3", SeatStatus.OCCUPIED));
        seatRepo.save(new Seat("s4", "room-3", "C-4", SeatStatus.MAINTENANCE));

        Response response = target("/rooms/room-3/seats").request(MediaType.APPLICATION_JSON).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seats = (List<Map<String, Object>>) body.get("seats");
        assertEquals(4, seats.size());

        List<String> statuses = seats.stream().map(s -> (String) s.get("status")).toList();
        assertTrue(statuses.contains("AVAILABLE"));
        assertTrue(statuses.contains("RESERVED"));
        assertTrue(statuses.contains("OCCUPIED"));
        assertTrue(statuses.contains("MAINTENANCE"));
    }

    @Test
    void shouldReturn200WithEmptySeatsForRoomWithNoSeats()
    {
        roomRepo.save(new StudyRoom("room-empty", "空自习室", "综合楼五楼", 10, RoomStatus.OPEN));

        Response response = target("/rooms/room-empty/seats").request(MediaType.APPLICATION_JSON).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("room-empty", body.get("roomId"));
        @SuppressWarnings("unchecked")
        List<?> seats = (List<?>) body.get("seats");
        assertTrue(seats.isEmpty());
    }
}
