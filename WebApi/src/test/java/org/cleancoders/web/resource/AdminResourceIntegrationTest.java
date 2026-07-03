package org.cleancoders.web.resource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.infrastructure.persistence.InMemoryRoomRepo;
import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;
import org.cleancoders.infrastructure.persistence.InMemoryTimeSlotRepo;
import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.web.binder.ReservationBinder;
import org.cleancoders.web.binder.SeatAndRoomBinder;
import org.cleancoders.web.binder.UserAndAuthBinder;
import org.cleancoders.web.binder.WebAppBinder;
import org.cleancoders.web.dto.admin.CreateRoomRequest;
import org.cleancoders.web.dto.admin.CreateSeatRequest;
import org.cleancoders.web.dto.admin.UpdateSeatRequest;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminResourceIntegrationTest extends JerseyTest
{

    private InMemoryRoomRepo roomRepo;
    private InMemorySeatRepo seatRepo;
    private JjwtTokenService tokenService;

    @Override
    protected Application configure()
    {
        roomRepo = new InMemoryRoomRepo();
        seatRepo = new InMemorySeatRepo();
        var timeSlotRepo = new InMemoryTimeSlotRepo();
        var userRepo = new InMemoryUserRepo();
        tokenService = new JjwtTokenService();

        userRepo.save(new User("admin-1", "admin", "admin123", UserRole.ADMIN, "Admin", "admin@example.com"));
        userRepo.save(new User("student-1", "alice", "pass123", UserRole.STUDENT, "Alice", "a@b.com"));

        var config = new ResourceConfig();
        config.register(RoomResource.class);
        config.register(AdminResource.class);
        config.register(WebAppBinder.class);
        config.register(SeatAndRoomBinder.class);
        config.register(UserAndAuthBinder.class);
        config.register(ReservationBinder.class);

        config.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(roomRepo).to(RoomRepository.class);
                bind(seatRepo).to(SeatRepository.class);
                bind(timeSlotRepo).to(TimeSlotRepository.class);
                bind(userRepo).to(UserRepository.class);
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

    @Test
    void shouldReturn201WhenAdminCreatesRoom()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(201, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertNotNull(body.get("id"));
        assertEquals("自习室F", body.get("name"));
        assertEquals("综合楼二楼", body.get("location"));
        assertEquals(20, body.get("capacity"));
        assertEquals("OPEN", body.get("status"));
    }

    @Test
    void shouldReturn403WhenStudentCreatesRoom()
    {
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn409WhenNameAlreadyExists()
    {
        roomRepo.save(new StudyRoom("r-existing", "自习室F", "图书馆一楼", 30, RoomStatus.OPEN));

        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室名称已存在", body.get("error"));
    }

    @Test
    void shouldReturn401WhenNoToken()
    {
        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(401, response.getStatus());
    }

    // --- update room tests ---

    @Test
    void shouldReturn200WhenAdminUpdatesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室A-改", "图书馆一楼东", 35)));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("room-1", body.get("id"));
        assertEquals("自习室A-改", body.get("name"));
        assertEquals("图书馆一楼东", body.get("location"));
        assertEquals(35, body.get("capacity"));
        assertEquals("OPEN", body.get("status"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonexistentRoom()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室X", "一楼", 10)));

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室不存在", body.get("error"));
        assertEquals("nonexistent", body.get("roomId"));
    }

    @Test
    void shouldReturn409WhenUpdatingToExistingName()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        roomRepo.save(new StudyRoom("room-2", "自习室B", "图书馆二楼", 20, RoomStatus.OPEN));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室B", "新位置", 25)));

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室名称已存在", body.get("error"));
    }

    @Test
    void shouldAllowUpdatingRoomWithSameName()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室A", "图书馆一楼东", 40)));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室A", body.get("name"));
        assertEquals("图书馆一楼东", body.get("location"));
        assertEquals(40, body.get("capacity"));
    }

    @Test
    void shouldReturn403WhenStudentUpdatesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .put(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(403, response.getStatus());
    }

    // --- create seat tests ---

    @Test
    void shouldReturn201WhenAdminCreatesSeat()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .post(Entity.json(new CreateSeatRequest("room-1", "A-9")));

        assertEquals(201, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertNotNull(body.get("id"));
        assertEquals("A-9", body.get("seatNumber"));
        assertEquals("AVAILABLE", body.get("status"));
    }

    @Test
    void shouldReturn404WhenRoomNotFoundForSeat()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .post(Entity.json(new CreateSeatRequest("nonexistent", "A-9")));

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室不存在", body.get("error"));
        assertEquals("nonexistent", body.get("roomId"));
    }

    @Test
    void shouldReturn409WhenSeatNumberAlreadyExists()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        // InMemorySeatRepo pre-seeds seat-1 ("A-1") in room-1
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .post(Entity.json(new CreateSeatRequest("room-1", "A-1")));

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位编号已存在", body.get("error"));
        assertEquals("room-1", body.get("roomId"));
        assertEquals("A-1", body.get("seatNumber"));
    }

    @Test
    void shouldReturn403WhenStudentCreatesSeat()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/seats")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(new CreateSeatRequest("room-1", "A-9")));

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn401WhenNoTokenForSeat()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/admin/seats")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(new CreateSeatRequest("room-1", "A-9")));

        assertEquals(401, response.getStatus());
    }

    // --- update seat tests ---

    @Test
    void shouldReturn200WhenAdminMarksSeatMaintenance()
    {
        // InMemorySeatRepo 预置 seat-1 (A-1, AVAILABLE) 在 room-1
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("seat-1", body.get("id"));
        assertEquals("A-1", body.get("seatNumber"));
        assertEquals("MAINTENANCE", body.get("status"));
    }

    @Test
    void shouldReturn200WhenAdminMarksSeatAvailable()
    {
        seatRepo.save(new Seat("seat-m", "room-1", "A-9", SeatStatus.MAINTENANCE));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-m")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("AVAILABLE")));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("seat-m", body.get("id"));
        assertEquals("AVAILABLE", body.get("status"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonexistentSeat()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位不存在", body.get("error"));
        assertEquals("nonexistent", body.get("seatId"));
    }

    @Test
    void shouldReturn400WhenStatusInvalid()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("BROKEN")));

        assertEquals(400, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("非法座位状态", body.get("error"));
        assertEquals("BROKEN", body.get("status"));
    }

    @Test
    void shouldReturn400WhenStatusNotAdminControllable()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("RESERVED")));

        assertEquals(400, response.getStatus());
    }

    @Test
    void shouldReturn409WhenIllegalTransition()
    {
        seatRepo.save(new Seat("seat-r", "room-1", "A-9", SeatStatus.RESERVED));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-r")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("非法状态转换", body.get("error"));
        assertEquals("RESERVED", body.get("currentStatus"));
        assertEquals("MAINTENANCE", body.get("targetStatus"));
    }

    @Test
    void shouldReturn403WhenStudentUpdatesSeat()
    {
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn401WhenNoTokenForSeatUpdate()
    {
        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(new UpdateSeatRequest("MAINTENANCE")));

        assertEquals(401, response.getStatus());
    }

    // --- delete room tests ---

    @Test
    void shouldReturn200WhenAdminDeletesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室已删除", body.get("message"));
        assertEquals("room-1", body.get("roomId"));

        // Verify room is now CLOSED
        StudyRoom room = roomRepo.findById("room-1").get();
        assertEquals(RoomStatus.CLOSED, room.status());
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentRoom()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(404, response.getStatus());
    }

    @Test
    void shouldReturn409WhenDeletingAlreadyClosedRoom()
    {
        roomRepo.save(new StudyRoom("room-closed", "已关闭", "三楼", 10, RoomStatus.CLOSED));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/rooms/room-closed")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
    }

    @Test
    void shouldReturn403WhenStudentDeletesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .delete();

        assertEquals(403, response.getStatus());
    }

    // --- delete seat tests ---

    @Test
    void shouldReturn200WhenAdminDeletesAvailableSeat()
    {
        seatRepo.save(new Seat("seat-del-av", "room-1", "X-A", SeatStatus.AVAILABLE));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-del-av")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位已删除", body.get("message"));
        assertEquals("seat-del-av", body.get("seatId"));

        // Verify seat is now REMOVED
        Seat seat = seatRepo.findById("seat-del-av").get();
        assertEquals(SeatStatus.REMOVED, seat.status());
    }

    @Test
    void shouldReturn200WhenAdminDeletesMaintenanceSeat()
    {
        seatRepo.save(new Seat("seat-del-mt", "room-1", "X-M", SeatStatus.MAINTENANCE));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-del-mt")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        assertEquals(SeatStatus.REMOVED, seatRepo.findById("seat-del-mt").get().status());
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentSeat()
    {
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位不存在", body.get("error"));
        assertEquals("nonexistent", body.get("seatId"));
    }

    @Test
    void shouldReturn409WhenSeatAlreadyRemoved()
    {
        seatRepo.save(new Seat("seat-removed", "room-1", "Z-1", SeatStatus.REMOVED));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-removed")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位已处于删除状态", body.get("error"));
        assertEquals("seat-removed", body.get("seatId"));
    }

    @Test
    void shouldReturn409WhenSeatReserved()
    {
        seatRepo.save(new Seat("seat-del-rs", "room-1", "X-R", SeatStatus.RESERVED));
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-del-rs")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位正在使用中，无法删除", body.get("error"));
        assertEquals("seat-del-rs", body.get("seatId"));
        assertEquals("RESERVED", body.get("currentStatus"));
    }

    @Test
    void shouldReturn409WhenSeatHasActiveReservations()
    {
        // seat-1: AVAILABLE but TestDataReservationRepo has RESERVED res-1
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位存在活跃预约，无法删除", body.get("error"));
        assertEquals("seat-1", body.get("seatId"));
    }

    @Test
    void shouldReturn200WhenSeatOnlyHasCancelledReservations()
    {
        // seat-2: AVAILABLE, res-6 is CANCELLED (not active)
        String adminToken = tokenService.generate("admin-1");

        Response response = target("/admin/seats/seat-2")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("座位已删除", body.get("message"));
        assertEquals("seat-2", body.get("seatId"));
    }

    @Test
    void shouldReturn403WhenStudentDeletesSeat()
    {
        seatRepo.save(new Seat("seat-del-st", "room-1", "X-S", SeatStatus.AVAILABLE));
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/seats/seat-del-st")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .delete();

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn401WhenNoTokenForSeatDelete()
    {
        seatRepo.save(new Seat("seat-del-no", "room-1", "X-N", SeatStatus.AVAILABLE));

        Response response = target("/admin/seats/seat-del-no")
                .request(MediaType.APPLICATION_JSON)
                .delete();

        assertEquals(401, response.getStatus());
    }
}
